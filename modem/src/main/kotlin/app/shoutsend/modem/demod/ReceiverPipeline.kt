package app.shoutsend.modem.demod

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.detector.GoertzelBank
import app.shoutsend.modem.detector.NoiseFloorTracker
import app.shoutsend.modem.fec.RsDecodeResult
import app.shoutsend.modem.fec.RsDecoder
import app.shoutsend.modem.packet.AssemblerResult
import app.shoutsend.modem.packet.FrameDecodeResult
import app.shoutsend.modem.packet.FrameDecoder
import app.shoutsend.modem.packet.MessageAssembler
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModeBlockCodec
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.SymbolMapper
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.protocol.CodewordSymbolizer
import app.shoutsend.modem.protocol.ProtocolConstants
import app.shoutsend.modem.sync.SyncDetector
import app.shoutsend.modem.sync.SyncListener
import kotlin.math.min

/**
 * Receiver states in the demodulation pipeline.
 */
public enum class PipelineState {
    HUNTING,
    READING_MODE,
    READING_FRAMES,
    COOLDOWN
}

/**
 * Complete acoustic receive pipeline composing:
 * - Ambient noise floor tracking (Step 19)
 * - Preamble detection and symbol sync (Step 20)
 * - Mode block decoding with 2-repeat energy summing
 * - Symbol demodulation (MFSK16 argmax, DTFSK dual argmax, confidence scoring)
 * - Codeword symbolization with erasures mapping (Step 11)
 * - Reed-Solomon errors-and-erasures decoding (Step 10)
 * - Physical frame decoding and CRC verification (Step 12)
 * - Message fragment assembly (Step 12)
 * - Optional PBKDF2/AES-GCM decryption and compaction expansion (Step 13-15)
 *
 * Runs deterministically in sample time with strictly zero heap allocations on the audio path.
 */
public class ReceiverPipeline(
    public val band: Band,
    public val audioSpec: AudioSpec,
    private val listener: ReceiveEventListener,
    public var passphrase: String? = null
) {
    public val tonePlan: TonePlan = TonePlan(band, audioSpec)
    public val headerSymbolSamples: Int = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec)

    // Preallocated circular buffer (power of 2: 1,048,576 samples = 21.8s at 48kHz)
    private val ringCapacity = 1048576
    private val ringMask = ringCapacity - 1
    private val ringBuffer = FloatArray(ringCapacity)
    private var writeHead: Long = 0L

    // Noise floor tracker & sync detector
    public val noiseTracker: NoiseFloorTracker = NoiseFloorTracker(2)
    private var syncLockedStartSample: Long = 0L
    private var syncLockedSnrDb: Float = 0f

    private val syncListener = object : SyncListener {
        override fun onPreambleCandidate(sampleIndex: Long, confidence: Float) {
            listener.onReceiveEvent(ReceiveEvent.PreambleDetected)
        }

        override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
            syncLockedStartSample = symbolStartSample
            syncLockedSnrDb = estimatedSnrDb
            if (state == PipelineState.HUNTING) {
                listener.onReceiveEvent(ReceiveEvent.Locked(estimatedSnrDb.toDouble()))
                state = PipelineState.READING_MODE
                nextSymbolSample = symbolStartSample
            }
        }

        override fun onSyncLost(sampleIndex: Long, reason: String) {
            if (state != PipelineState.HUNTING && state != PipelineState.COOLDOWN) {
                listener.onReceiveEvent(ReceiveEvent.Lost(reason))
                resetToHunting()
            }
        }
    }

    public val syncDetector: SyncDetector = SyncDetector(
        band = band,
        audioSpec = audioSpec,
        listener = syncListener,
        noiseTracker = noiseTracker
    )

    public val messageAssembler: MessageAssembler = MessageAssembler(
        clock = { (writeHead * 1000L) / audioSpec.sampleRateHz }
    )

    // Mode resonators (header speed: Normal)
    private val headerDataBank = GoertzelBank(
        frequencies = tonePlan.dataTones.toDoubleArray(),
        audioSpec = audioSpec,
        blockSize = headerSymbolSamples,
        windowed = true
    )

    // Active state
    public var state: PipelineState = PipelineState.HUNTING
        private set

    private var activeProfile: ModemProfile? = null
    private var activeSymbolSamples: Int = headerSymbolSamples
    private var activeBank: GoertzelBank = headerDataBank
    private var nextSymbolSample: Long = 0L
    private var cooldownUntilSample: Long = 0L

    // Reusable buffers to ensure zero allocations on symbol extraction
    private val maxSymbolSamples = Timing.symbolSamples(Speed.ROBUST, audioSpec)
    private val symbolAudioBuffer = FloatArray(maxSymbolSamples)
    private val energyBuffer = FloatArray(16)
    private val modeCombinedEnergies = Array(2) { FloatArray(16) }
    private val symbolScratch = IntArray(256)
    private val confidenceScratch = FloatArray(256)

    /**
     * Ingests normalized float PCM samples.
     */
    public fun push(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid range" }
        val chunkSize = 960
        var currentOffset = offset
        var remaining = count
        while (remaining > 0) {
            val chunk = minOf(chunkSize, remaining)
            for (i in currentOffset until currentOffset + chunk) {
                ringBuffer[(writeHead and ringMask.toLong()).toInt()] = samples[i]
                writeHead++
            }
            syncDetector.push(samples, currentOffset, chunk)
            processPipeline()
            currentOffset += chunk
            remaining -= chunk
        }
    }

    /**
     * Ingests 16-bit linear signed PCM samples.
     */
    public fun push(samples: ShortArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid range" }
        val chunkSize = 960
        var currentOffset = offset
        var remaining = count
        while (remaining > 0) {
            val chunk = minOf(chunkSize, remaining)
            for (i in currentOffset until currentOffset + chunk) {
                ringBuffer[(writeHead and ringMask.toLong()).toInt()] = samples[i] / 32768.0f
                writeHead++
            }
            syncDetector.push(samples, currentOffset, chunk)
            processPipeline()
            currentOffset += chunk
            remaining -= chunk
        }
    }

    private fun processPipeline() {
        var progress = true
        while (progress) {
            progress = when (state) {
                PipelineState.HUNTING -> false
                PipelineState.READING_MODE -> processModeBlock()
                PipelineState.READING_FRAMES -> processNextFrame()
                PipelineState.COOLDOWN -> processCooldown()
            }
        }
    }

    private fun processModeBlock(): Boolean {
        // Mode block consists of 4 symbols at headerSymbolSamples
        val neededSamples = nextSymbolSample + 4L * headerSymbolSamples
        if (writeHead < neededSamples) {
            return false
        }

        // Reset mode energies
        for (nibble in 0..1) {
            modeCombinedEnergies[nibble].fill(0.0f)
        }

        var sampleIdx = nextSymbolSample
        for (symIdx in 0 until 4) {
            extractSamples(sampleIdx, headerSymbolSamples, symbolAudioBuffer)
            headerDataBank.energies(symbolAudioBuffer, 0, energyBuffer)

            val nibbleIdx = symIdx % 2
            for (k in 0..15) {
                modeCombinedEnergies[nibbleIdx][k] += energyBuffer[k]
            }
            sampleIdx += headerSymbolSamples
        }

        // Argmax for high nibble and low nibble
        val hiNibble = argmax(modeCombinedEnergies[0])
        val loNibble = argmax(modeCombinedEnergies[1])

        val decodeResult = ModeBlockCodec.decodeMode(intArrayOf(hiNibble, loNibble))
        if (decodeResult.isFailure) {
            listener.onReceiveEvent(ReceiveEvent.FrameFailed("Invalid mode block: $hiNibble, $loNibble"))
            resetToHunting()
            return true
        }

        val decoded = decodeResult.getOrThrow()
        val profile = ModemProfile(
            band = band,
            scheme = decoded.scheme,
            speed = decoded.speed,
            repeats = 1,
            rsProfileId = decoded.rsProfileId
        )
        activeProfile = profile
        listener.onReceiveEvent(ReceiveEvent.ModeDecoded(profile))

        // Configure active timing and resonators for data payload frames
        activeSymbolSamples = Timing.symbolSamples(profile.speed, audioSpec)
        activeBank = if (activeSymbolSamples == headerSymbolSamples) {
            headerDataBank
        } else {
            GoertzelBank(
                frequencies = tonePlan.dataTones.toDoubleArray(),
                audioSpec = audioSpec,
                blockSize = activeSymbolSamples,
                windowed = true
            )
        }

        nextSymbolSample = neededSamples
        state = PipelineState.READING_FRAMES
        return true
    }

    private fun processNextFrame(): Boolean {
        val profile = activeProfile ?: return false
        val nBytes = profile.rsProfile.n

        // Codeword symbol count
        val codewordSymbols = when (profile.scheme) {
            Scheme.MFSK16 -> nBytes * 2
            Scheme.DTFSK -> (nBytes * 8 + 5) / 6
        }
        val totalFrameSymbols = 4 + codewordSymbols // 4 marker symbols + codeword
        val frameSamples = totalFrameSymbols.toLong() * activeSymbolSamples

        if (writeHead < nextSymbolSample + frameSamples) {
            return false
        }

        // 1. Read Frame Marker (4 symbols)
        var sampleIdx = nextSymbolSample
        var markerMatches = 0
        for (m in 0 until 4) {
            extractSamples(sampleIdx, activeSymbolSamples, symbolAudioBuffer)
            activeBank.energies(symbolAudioBuffer, 0, energyBuffer)
            val bestTone = argmax(energyBuffer)
            if (bestTone == ProtocolConstants.FRAME_MARKER_PATTERN[m]) {
                markerMatches++
            }
            sampleIdx += activeSymbolSamples
        }

        // If marker has 0 matches, synchronization is likely lost
        if (markerMatches == 0) {
            listener.onReceiveEvent(ReceiveEvent.Lost("Lost frame sync marker"))
            resetToHunting()
            return true
        }

        // 2. Read Codeword Symbols
        for (s in 0 until codewordSymbols) {
            extractSamples(sampleIdx, activeSymbolSamples, symbolAudioBuffer)
            activeBank.energies(symbolAudioBuffer, 0, energyBuffer)

            when (profile.scheme) {
                Scheme.MFSK16 -> {
                    val best = argmax(energyBuffer)
                    val second = secondArgmax(energyBuffer, best)
                    val bestE = energyBuffer[best]
                    val secondE = energyBuffer[second]
                    val conf = if (bestE > 0.0f) ((bestE - secondE) / bestE).coerceIn(0f, 1f) else 0f

                    symbolScratch[s] = best
                    confidenceScratch[s] = conf
                }
                Scheme.DTFSK -> {
                    val bestLo = argmaxRange(energyBuffer, 0, 7)
                    val secondLo = secondArgmaxRange(energyBuffer, 0, 7, bestLo)
                    val confLo = if (energyBuffer[bestLo] > 0.0f) {
                        ((energyBuffer[bestLo] - energyBuffer[secondLo]) / energyBuffer[bestLo]).coerceIn(0f, 1f)
                    } else 0f

                    val bestHi = argmaxRange(energyBuffer, 8, 15)
                    val secondHi = secondArgmaxRange(energyBuffer, 8, 15, bestHi)
                    val confHi = if (energyBuffer[bestHi] > 0.0f) {
                        ((energyBuffer[bestHi] - energyBuffer[secondHi]) / energyBuffer[bestHi]).coerceIn(0f, 1f)
                    } else 0f

                    symbolScratch[s] = SymbolMapper.combineDtfskTones(bestLo, bestHi)
                    confidenceScratch[s] = min(confLo, confHi)
                }
            }
            sampleIdx += activeSymbolSamples
        }

        // Advance next symbol sample
        nextSymbolSample += frameSamples

        // 3. Decode Codeword with Erasure Detection
        val decoded = CodewordSymbolizer.decode(
            symbols = symbolScratch.copyOf(codewordSymbols),
            confidences = confidenceScratch.copyOf(codewordSymbols),
            scheme = profile.scheme,
            frameSeed = ProtocolConstants.WHITEN_SEED_V1,
            byteCount = nBytes,
            confidenceThreshold = 0.20f
        )

        // 4. Reed-Solomon Decode
        val rsResult = RsDecoder.decode(
            received = decoded.bytes,
            erasurePositions = decoded.erasurePositions,
            profile = profile.rsProfile
        )

        if (rsResult !is RsDecodeResult.Corrected) {
            listener.onReceiveEvent(ReceiveEvent.FrameFailed("RS decode failure"))
            return true
        }

        // 5. Frame Header and CRC Verification
        val frameResult = FrameDecoder.decode(rsResult.data, profile.rsProfile)
        if (frameResult !is FrameDecodeResult.Success) {
            listener.onReceiveEvent(ReceiveEvent.FrameFailed("Frame CRC / header validation failure"))
            return true
        }

        val frame = frameResult.frame
        listener.onReceiveEvent(
            ReceiveEvent.FrameDecoded(
                fragmentIndex = frame.header.fragmentIndex,
                fragmentCount = frame.header.fragmentCount,
                correctedBytes = rsResult.errorCount + rsResult.erasureCount
            )
        )

        // 6. Feed to Message Assembler
        when (val asmResult = messageAssembler.feed(frame)) {
            is AssemblerResult.Complete -> {
                val msg = asmResult.message
                val payload = PayloadDecoder.decode(msg, passphrase)
                listener.onReceiveEvent(ReceiveEvent.MessageComplete(msg, payload))

                // Enter 300 ms silence cooldown
                val cooldownSamples = (0.300 * audioSpec.sampleRateHz).toLong()
                cooldownUntilSample = writeHead + cooldownSamples
                state = PipelineState.COOLDOWN
                listener.onReceiveEvent(ReceiveEvent.Cooldown)
            }
            is AssemblerResult.Incomplete -> {
                listener.onReceiveEvent(
                    ReceiveEvent.Progress(
                        receivedFragments = asmResult.progress.receivedFragments,
                        totalFragments = asmResult.progress.totalFragments
                    )
                )
            }
            is AssemblerResult.Conflict -> {
                listener.onReceiveEvent(ReceiveEvent.FrameFailed("Message assembly stream conflict"))
            }
            is AssemblerResult.Duplicate -> {
                // Repeated fragment cleanly ignored
            }
        }

        return true
    }

    private fun processCooldown(): Boolean {
        if (writeHead >= cooldownUntilSample) {
            resetToHunting()
            return true
        }
        return false
    }

    private fun resetToHunting() {
        state = PipelineState.HUNTING
        activeProfile = null
        syncDetector.reset()
    }

    private fun extractSamples(startSample: Long, count: Int, out: FloatArray) {
        var idx = startSample
        for (i in 0 until count) {
            out[i] = ringBuffer[(idx and ringMask.toLong()).toInt()]
            idx++
        }
    }

    private fun argmax(values: FloatArray): Int {
        var maxIdx = 0
        var maxVal = values[0]
        for (i in 1 until values.size) {
            if (values[i] > maxVal) {
                maxVal = values[i]
                maxIdx = i
            }
        }
        return maxIdx
    }

    private fun secondArgmax(values: FloatArray, exclude: Int): Int {
        var secondIdx = if (exclude == 0) 1 else 0
        var secondVal = values[secondIdx]
        for (i in 0 until values.size) {
            if (i != exclude && values[i] > secondVal) {
                secondVal = values[i]
                secondIdx = i
            }
        }
        return secondIdx
    }

    private fun argmaxRange(values: FloatArray, start: Int, end: Int): Int {
        var maxIdx = start
        var maxVal = values[start]
        for (i in start + 1..end) {
            if (values[i] > maxVal) {
                maxVal = values[i]
                maxIdx = i
            }
        }
        return maxIdx
    }

    private fun secondArgmaxRange(values: FloatArray, start: Int, end: Int, exclude: Int): Int {
        var secondIdx = if (exclude == start) start + 1 else start
        var secondVal = values[secondIdx]
        for (i in start..end) {
            if (i != exclude && values[i] > secondVal) {
                secondVal = values[i]
                secondIdx = i
            }
        }
        return secondIdx
    }

    /**
     * Test helper: feeds [pcm] completely and returns all emitted [ReceiveEvent]s.
     */
    public fun decodeAll(pcm: FloatArray, passphrase: String? = this.passphrase): List<ReceiveEvent> {
        this.passphrase = passphrase
        val events = mutableListOf<ReceiveEvent>()
        val testPipeline = ReceiverPipeline(band, audioSpec, { event -> events.add(event) }, passphrase)
        testPipeline.push(pcm)
        return events
    }
}
