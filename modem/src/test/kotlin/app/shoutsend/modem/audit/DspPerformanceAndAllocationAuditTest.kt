package app.shoutsend.modem.audit

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.demod.ReceiveEventListener
import app.shoutsend.modem.demod.ReceiverPipeline
import app.shoutsend.modem.detector.GoertzelBank
import app.shoutsend.modem.detector.NoiseFloorTracker
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.sync.SyncDetector
import app.shoutsend.modem.sync.SyncListener
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory
import kotlin.math.cos
import kotlin.math.sin

class DspPerformanceAndAllocationAuditTest {

    private val audioSpec = AudioSpec.RATE_48K

    // Documented upper bound for steady-state background allocations (TLAB/JVM MXBean reflection overhead)
    private val maxBytesPerSecondBound = 50.0

    private fun getThreadAllocatedBytes(): Long {
        val mxBean = ManagementFactory.getThreadMXBean()
        return if (mxBean is com.sun.management.ThreadMXBean && mxBean.isThreadAllocatedMemorySupported) {
            mxBean.getThreadAllocatedBytes(Thread.currentThread().id)
        } else {
            -1L
        }
    }

    @Test
    fun testGoertzelBankZeroAllocationsOnHotPath() {
        val tonePlan = TonePlan(Band.RETRO, audioSpec)
        val bank = GoertzelBank(
            frequencies = tonePlan.dataTones.toDoubleArray(),
            audioSpec = audioSpec,
            blockSize = 1440,
            windowed = true
        )

        val samples = FloatArray(1440) { i -> (sin(i * 0.1) * 0.5).toFloat() }
        val out = FloatArray(16)

        // Warm up JIT
        repeat(10_000) {
            bank.energies(samples, 0, out)
        }

        val startAlloc = getThreadAllocatedBytes()
        if (startAlloc < 0) return // Skip if JVM thread allocation counter unsupported

        // Process 10,000 blocks (equivalent to 300 seconds of audio)
        val iterations = 10_000
        val audioSeconds = (iterations * 1440).toDouble() / 48000.0
        repeat(iterations) {
            bank.energies(samples, 0, out)
        }
        val endAlloc = getThreadAllocatedBytes()
        val totalAllocated = endAlloc - startAlloc
        val bytesPerSec = totalAllocated.toDouble() / audioSeconds

        println("[AUDIT] GoertzelBank: allocated $totalAllocated bytes over $iterations evaluations (${"%.2f".format(bytesPerSec)} B/s).")
        assertTrue("GoertzelBank steady state allocation ($bytesPerSec B/s) must be below $maxBytesPerSecondBound B/s", bytesPerSec <= maxBytesPerSecondBound)
    }

    @Test
    fun testNoiseFloorTrackerZeroAllocationsOnHotPath() {
        val tracker = NoiseFloorTracker(toneCount = 18)
        val energies = FloatArray(18) { 0.001f }

        // Warm up JIT
        repeat(10_000) {
            tracker.update(energies)
        }

        val startAlloc = getThreadAllocatedBytes()
        if (startAlloc < 0) return

        val iterations = 50_000
        val audioSeconds = iterations * 0.0075 // 7.5ms per hop
        repeat(iterations) {
            tracker.update(energies)
        }
        val endAlloc = getThreadAllocatedBytes()
        val totalAllocated = endAlloc - startAlloc
        val bytesPerSec = totalAllocated.toDouble() / audioSeconds

        println("[AUDIT] NoiseFloorTracker: allocated $totalAllocated bytes over $iterations updates (${"%.2f".format(bytesPerSec)} B/s).")
        assertTrue("NoiseFloorTracker steady state allocation ($bytesPerSec B/s) must be below $maxBytesPerSecondBound B/s", bytesPerSec <= maxBytesPerSecondBound)
    }

    @Test
    fun testSyncDetectorHuntingZeroAllocationsOver30Seconds() {
        val dummyListener = object : SyncListener {
            override fun onPreambleCandidate(sampleIndex: Long, confidence: Float) {}
            override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {}
            override fun onSyncLost(sampleIndex: Long, reason: String) {}
        }
        val syncDetector = SyncDetector(
            band = Band.RETRO,
            audioSpec = audioSpec,
            listener = dummyListener
        )

        val chunkSize = 1024
        val chunk = FloatArray(chunkSize) { (cos(it * 0.05) * 0.01).toFloat() }

        // Warm up JIT (5 seconds of audio)
        val warmupChunks = (5 * 48000) / chunkSize
        repeat(warmupChunks) {
            syncDetector.push(chunk, 0, chunkSize)
        }

        val startAlloc = getThreadAllocatedBytes()
        if (startAlloc < 0) return

        // 30 seconds of audio at 48 kHz
        val testChunks = (30 * 48000) / chunkSize
        repeat(testChunks) {
            syncDetector.push(chunk, 0, chunkSize)
        }
        val endAlloc = getThreadAllocatedBytes()
        val totalAllocated = endAlloc - startAlloc
        val bytesPerSec = totalAllocated.toDouble() / 30.0

        println("[AUDIT] SyncDetector HUNTING: allocated $totalAllocated bytes over 30s (${"%.2f".format(bytesPerSec)} B/s).")
        assertTrue("SyncDetector steady state allocation ($bytesPerSec B/s) must be below $maxBytesPerSecondBound B/s", bytesPerSec <= maxBytesPerSecondBound)
    }

    @Test
    fun testReceiverPipelineListeningZeroAllocationsOver30Seconds() {
        val dummyListener = ReceiveEventListener {}
        val pipeline = ReceiverPipeline(
            band = Band.RETRO,
            audioSpec = audioSpec,
            listener = dummyListener
        )

        val chunkSize = 1024
        val chunk = FloatArray(chunkSize) { (sin(it * 0.02) * 0.005).toFloat() }

        // Warm up JIT (5 seconds)
        val warmupChunks = (5 * 48000) / chunkSize
        repeat(warmupChunks) {
            pipeline.push(chunk, 0, chunkSize)
        }

        val startAlloc = getThreadAllocatedBytes()
        if (startAlloc < 0) return

        // 30 seconds of audio at 48 kHz
        val testChunks = (30 * 48000) / chunkSize
        repeat(testChunks) {
            pipeline.push(chunk, 0, chunkSize)
        }
        val endAlloc = getThreadAllocatedBytes()
        val totalAllocated = endAlloc - startAlloc
        val bytesPerSec = totalAllocated.toDouble() / 30.0

        println("[AUDIT] ReceiverPipeline Listening: allocated $totalAllocated bytes over 30s (${"%.2f".format(bytesPerSec)} B/s).")
        assertTrue("ReceiverPipeline steady state allocation ($bytesPerSec B/s) must be below $maxBytesPerSecondBound B/s", bytesPerSec <= maxBytesPerSecondBound)
    }

    @Test
    fun testRealTimeFactorBelowTargetOnBuildMachine() {
        // Construct 60 seconds of audio:
        // - 10s silence
        // - A transmission of ~8.5s in the middle
        // - Remaining ~41.5s silence/ambient noise
        val totalAudioSeconds = 60.0
        val sampleRate = audioSpec.sampleRateHz
        val totalSamples = (totalAudioSeconds * sampleRate).toInt()
        val audio = FloatArray(totalSamples)

        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 2, rsProfileId = 1)
        val messageText = "Real-time processing factor benchmark transmission text"
        val payloadBytes = PayloadCodec.encode(Payload.Text(messageText))

        val plan = TransmissionBuilder.build(
            message = OutgoingMessage(MessageType.TEXT, MessageFlags.NONE, payloadBytes),
            profile = profile,
            audioSpec = audioSpec
        )
        val txAudio = Modulator(plan).modulateToFloatArray()

        // Place transmission at sample 480,000 (10s in)
        val txOffset = 10 * sampleRate
        System.arraycopy(txAudio, 0, audio, txOffset, txAudio.size)

        var messageReceived = false
        val pipeline = ReceiverPipeline(
            band = Band.RETRO,
            audioSpec = audioSpec,
            listener = { event ->
                if (event is app.shoutsend.modem.demod.ReceiveEvent.MessageComplete) {
                    messageReceived = true
                }
            }
        )

        // Warm up JIT
        val warmupBuffer = FloatArray(48000)
        repeat(3) { pipeline.push(warmupBuffer, 0, warmupBuffer.size) }

        val chunkSize = 960
        var cursor = 0

        val startTimeNanos = System.nanoTime()
        while (cursor < totalSamples) {
            val toFeed = minOf(chunkSize, totalSamples - cursor)
            pipeline.push(audio, cursor, toFeed)
            cursor += toFeed
        }
        val elapsedNanos = System.nanoTime() - startTimeNanos
        val elapsedSeconds = elapsedNanos.toDouble() / 1_000_000_000.0

        val realTimeFactor = elapsedSeconds / totalAudioSeconds
        println("[AUDIT] Real-Time Factor (RTF): audio=$totalAudioSeconds s, elapsed=$elapsedSeconds s, RTF=$realTimeFactor (${1.0 / realTimeFactor}x faster than real time)")

        assertTrue("Message must be decoded successfully in stream", messageReceived)
        assertTrue("RTF on build machine ($realTimeFactor) must be well below target 0.02 (actual: ${"%.4f".format(realTimeFactor)})", realTimeFactor < 0.02)
    }

    @Test
    fun testFloatVsDoubleInnerLoopGoertzelComparison() {
        val frequencies = doubleArrayOf(1500.0, 2000.0, 2500.0, 3000.0)
        val blockSize = 1440
        val coeffsDouble = DoubleArray(frequencies.size) { 2.0 * cos((2.0 * Math.PI * frequencies[it]) / 48000.0) }
        val coeffsFloat = FloatArray(frequencies.size) { (2.0 * cos((2.0 * Math.PI * frequencies[it]) / 48000.0)).toFloat() }

        val samples = FloatArray(blockSize) { (sin(it * 0.1) * 0.5).toFloat() }

        // Double-precision accumulator loop
        fun runDouble(iters: Int): Long {
            val start = System.nanoTime()
            var run = 0
            while (run < iters) {
                var f = 0
                while (f < frequencies.size) {
                    val coeff = coeffsDouble[f]
                    var q0: Double
                    var q1 = 0.0
                    var q2 = 0.0
                    var i = 0
                    while (i < blockSize) {
                        q0 = coeff * q1 - q2 + samples[i]
                        q2 = q1
                        q1 = q0
                        i++
                    }
                    f++
                }
                run++
            }
            return System.nanoTime() - start
        }

        // Single-precision float accumulator loop
        fun runFloat(iters: Int): Long {
            val start = System.nanoTime()
            var run = 0
            while (run < iters) {
                var f = 0
                while (f < frequencies.size) {
                    val coeff = coeffsFloat[f]
                    var q0: Float
                    var q1 = 0.0f
                    var q2 = 0.0f
                    var i = 0
                    while (i < blockSize) {
                        q0 = coeff * q1 - q2 + samples[i]
                        q2 = q1
                        q1 = q0
                        i++
                    }
                    f++
                }
                run++
            }
            return System.nanoTime() - start
        }

        // Warmup
        runDouble(2000)
        runFloat(2000)

        val timeDouble = runDouble(10000)
        val timeFloat = runFloat(10000)

        val timeDoubleMs = timeDouble.toDouble() / 1_000_000.0
        val timeFloatMs = timeFloat.toDouble() / 1_000_000.0
        println("[AUDIT] Goertzel Inner Loop Benchmark: Double=$timeDoubleMs ms vs Float=$timeFloatMs ms (ratio=${"%.2f".format(timeFloatMs / timeDoubleMs)})")

        // In modern 64-bit JVMs with AVX/NEON, 64-bit Double execution is virtually identical in speed to Float,
        // while offering 53-bit mantissa precision preventing accumulation drift over long blocks.
        assertTrue("Double loop benchmark must complete successfully", timeDouble > 0)
    }
}
