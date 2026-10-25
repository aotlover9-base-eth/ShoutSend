package app.shoutsend.modem.modulator

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.fec.RsEncoder
import app.shoutsend.modem.packet.FrameEncoder
import app.shoutsend.modem.packet.Fragmenter
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.ModeBlockCodec
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.SymbolMapper
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.protocol.CodewordSymbolizer
import app.shoutsend.modem.protocol.ProtocolConstants
import app.shoutsend.modem.synth.SymbolEvent
import app.shoutsend.modem.synth.SymbolSchedule
import app.shoutsend.modem.synth.ToneSynth
import app.shoutsend.modem.tuning.ModemTuning

/**
 * Builds physical transmission plans from outgoing application messages and modem configurations.
 *
 * Implements Section 6 of Protocol v1:
 * 1. Hardware lead-in silence (default 250 ms)
 * 2. 16-symbol acquisition preamble (alternating S0 and S1 at Normal 30ms timing)
 * 3. 8-symbol synchronization word (fixed pseudo-random MFSK pattern at Normal 30ms timing)
 * 4. 4-symbol mode block (2 symbols repeated once declaring scheme, speed, RS profile at Normal 30ms timing)
 * 5. Physical frames repeated R times (default 2):
 *    - 4-symbol frame marker at active speed
 *    - Whitened Reed-Solomon codeword symbols at active speed and scheme
 * 6. Hardware cooldown tail silence (default 150 ms)
 */
public object TransmissionBuilder {

    /** Default lead-in silence in milliseconds. */
    public const val DEFAULT_LEAD_IN_SILENCE_MS: Int = 250

    /** Default tail silence in milliseconds. */
    public const val DEFAULT_TAIL_SILENCE_MS: Int = 150

    /**
     * Builds a [TransmissionPlan] from an [OutgoingMessage].
     */
    public fun build(
        message: OutgoingMessage,
        profile: ModemProfile,
        audioSpec: AudioSpec,
        tuning: ModemTuning = ModemTuning.DEFAULT,
        leadInSilenceMs: Int = tuning.leadInSilenceMs,
        tailSilenceMs: Int = tuning.tailSilenceMs,
        outputLevel: Float = tuning.outputLevel,
        repeats: Int = profile.repeats
    ): TransmissionPlan {
        require(leadInSilenceMs >= 0) { "leadInSilenceMs cannot be negative: $leadInSilenceMs" }
        require(tailSilenceMs >= 0) { "tailSilenceMs cannot be negative: $tailSilenceMs" }
        require(outputLevel > 0.0f && outputLevel <= 1.0f) { "outputLevel must be in (0.0, 1.0], got $outputLevel" }
        require(repeats in 1..3) { "repeats must be in 1..3, got $repeats" }

        val rsProfile = profile.rsProfile
        val frames = Fragmenter.fragment(
            payload = message.payload,
            type = message.type,
            flags = message.flags,
            profile = rsProfile,
            messageId = message.messageId
        )

        val codewords = ArrayList<ByteArray>(frames.size)
        for (frame in frames) {
            val dataBlock = FrameEncoder.encode(frame, rsProfile)
            val codeword = RsEncoder.encode(dataBlock, rsProfile)
            codewords.add(codeword)
        }

        val tonePlan = TonePlan(profile.band, audioSpec)
        val leadInSamples = (leadInSilenceMs.toLong() * audioSpec.sampleRateHz) / 1000L
        var currentSample = leadInSamples
        val events = ArrayList<SymbolEvent>()

        // 1. Header Timing (Normal speed: 30ms duration, 4ms ramp, MFSK16)
        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec)
        val headerRampSamples = Timing.rampSamples(ProtocolConstants.HEADER_SPEED, audioSpec)
        val headerSymLength = headerDurSamples + headerRampSamples

        // 1a. Acquisition Preamble (16 symbols alternating S0 and S1)
        for (tone in ProtocolConstants.PREAMBLE_PATTERN) {
            events.add(
                SymbolEvent(
                    startSample = currentSample,
                    lengthSamples = headerSymLength,
                    rampSamples = headerRampSamples,
                    toneIndexes = intArrayOf(tone),
                    levelPerTone = outputLevel
                )
            )
            currentSample += headerDurSamples
        }

        // 1b. Sync Word (8 symbols)
        for (tone in ProtocolConstants.SYNC_WORD_PATTERN) {
            events.add(
                SymbolEvent(
                    startSample = currentSample,
                    lengthSamples = headerSymLength,
                    rampSamples = headerRampSamples,
                    toneIndexes = intArrayOf(tone),
                    levelPerTone = outputLevel
                )
            )
            currentSample += headerDurSamples
        }

        // 1c. Mode Block (2 symbols repeated once = 4 symbols)
        val modeSymbols = ModeBlockCodec.encodeMode(profile.scheme, profile.speed, profile.rsProfileId)
        for (rep in 0 until 2) {
            for (tone in modeSymbols) {
                events.add(
                    SymbolEvent(
                        startSample = currentSample,
                        lengthSamples = headerSymLength,
                        rampSamples = headerRampSamples,
                        toneIndexes = intArrayOf(tone),
                        levelPerTone = outputLevel
                    )
                )
                currentSample += headerDurSamples
            }
        }

        // 2. Active Frame Timing (profile.speed and profile.scheme)
        val payloadDurMs = tuning.durationMs(profile.speed)
        val payloadRampMs = tuning.rampMs(profile.speed)
        val payloadDurSamples = Timing.symbolSamples(payloadDurMs, audioSpec)
        val payloadRampSamples = Timing.rampSamples(payloadRampMs, audioSpec)
        val payloadSymLength = payloadDurSamples + payloadRampSamples
        val isDtfsk = profile.scheme == Scheme.DTFSK
        val payloadLevelPerTone = if (isDtfsk) outputLevel / 2.0f else outputLevel

        for (rep in 0 until repeats) {
            for (f in frames.indices) {
                val codeword = codewords[f]

                // 2a. Frame Marker (4 symbols at active speed)
                for (markerTone in ProtocolConstants.FRAME_MARKER_PATTERN) {
                    events.add(
                        SymbolEvent(
                            startSample = currentSample,
                            lengthSamples = payloadSymLength,
                            rampSamples = payloadRampSamples,
                            toneIndexes = intArrayOf(markerTone),
                            levelPerTone = outputLevel
                        )
                    )
                    currentSample += payloadDurSamples
                }

                // 2b. Codeword Symbols (at active speed & scheme)
                val symbols = CodewordSymbolizer.encode(
                    codeword = codeword,
                    scheme = profile.scheme,
                    frameSeed = ProtocolConstants.WHITEN_SEED_V1
                )

                for (sym in symbols) {
                    val tones = if (isDtfsk) {
                        val (low, high) = SymbolMapper.splitDtfskTones(sym)
                        intArrayOf(low, high)
                    } else {
                        intArrayOf(sym)
                    }

                    events.add(
                        SymbolEvent(
                            startSample = currentSample,
                            lengthSamples = payloadSymLength,
                            rampSamples = payloadRampSamples,
                            toneIndexes = tones,
                            levelPerTone = payloadLevelPerTone
                        )
                    )
                    currentSample += payloadDurSamples
                }
            }
        }

        val schedule = SymbolSchedule(events)
        val tailSilenceSamples = (tailSilenceMs.toLong() * audioSpec.sampleRateHz) / 1000L
        val totalSamples = schedule.totalSamples + tailSilenceSamples

        return TransmissionPlan(
            message = message,
            frames = frames,
            codewords = codewords,
            schedule = schedule,
            profile = profile,
            tonePlan = tonePlan,
            audioSpec = audioSpec,
            leadInSilenceMs = leadInSilenceMs,
            tailSilenceMs = tailSilenceMs,
            totalSamples = totalSamples
        )
    }

    /**
     * Builds a [TransmissionPlan] from raw message parameters.
     */
    public fun build(
        type: MessageType,
        flags: MessageFlags = MessageFlags.NONE,
        payload: ByteArray,
        profile: ModemProfile,
        audioSpec: AudioSpec,
        messageId: Int = (0..255).random(),
        leadInSilenceMs: Int = DEFAULT_LEAD_IN_SILENCE_MS,
        tailSilenceMs: Int = DEFAULT_TAIL_SILENCE_MS,
        outputLevel: Float = ToneSynth.DEFAULT_OUTPUT_LEVEL,
        repeats: Int = profile.repeats
    ): TransmissionPlan {
        return build(
            message = OutgoingMessage(type = type, flags = flags, payload = payload, messageId = messageId),
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = leadInSilenceMs,
            tailSilenceMs = tailSilenceMs,
            outputLevel = outputLevel,
            repeats = repeats
        )
    }
}
