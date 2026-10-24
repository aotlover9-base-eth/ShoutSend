package app.shoutsend.modem.channel

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.demod.ReceiverPipeline
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.ModemProfile

/**
 * Result metrics returned by [LoopbackHarness.run].
 *
 * @property success True if a transmission was received and reconstructed.
 * @property exactPayloadMatch True if the delivered payload bytes match the transmitted bytes.
 * @property timeToLockMs Duration in milliseconds from start of audio until synchronization lock.
 * @property symbolErrorRate Approximate symbol error rate or 0.0 if transmission succeeded with zero corrections.
 * @property totalRsCorrections Total number of bytes corrected across all frames by Reed-Solomon.
 * @property framesLost Total number of physical frames that failed validation or decoding.
 * @property falseLocks Total number of spurious synchronization locks that failed to decode a mode block.
 * @property durationSeconds Total acoustic audio duration in fractional seconds.
 * @property receivedMessage Reconstructed wire message, or null if lost.
 * @property receivedPayload Reconstructed and deserialized application payload, or null if lost.
 */
public data class LoopbackResult(
    public val success: Boolean,
    public val exactPayloadMatch: Boolean,
    public val timeToLockMs: Double?,
    public val symbolErrorRate: Double,
    public val totalRsCorrections: Int,
    public val framesLost: Int,
    public val falseLocks: Int,
    public val durationSeconds: Double,
    public val receivedMessage: ReceivedMessage?,
    public val receivedPayload: Payload?
)

/**
 * End-to-end acoustic simulation harness.
 *
 * Transmits an [OutgoingMessage] through [Modulator], subjects the synthesized PCM
 * to physical impairments via [AcousticChannelSimulator], and decodes using [ReceiverPipeline].
 */
public object LoopbackHarness {

    /**
     * Executes an end-to-end loopback transmission under [scenario] with deterministic [seed].
     */
    public fun run(
        message: OutgoingMessage,
        profile: ModemProfile,
        scenario: ChannelScenario = ChannelScenario.CLEAN,
        audioSpec: AudioSpec = AudioSpec.RATE_48K,
        seed: Long = 42L,
        passphrase: String? = null
    ): LoopbackResult {
        // 1. Modulate pristine acoustic signal
        val plan = TransmissionBuilder.build(
            message = message,
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 50,
            tailSilenceMs = 150
        )
        val pristinePcm = Modulator(plan).modulateToFloatArray()

        // 2. Pass through acoustic channel simulator
        val simulator = AcousticChannelSimulator(scenario, audioSpec)
        val impairedPcm = simulator.process(pristinePcm, seed)

        // 3. Receive through ReceiverPipeline
        val events = ArrayList<ReceiveEvent>()
        var lockedSample: Long? = null
        var rsCorrections = 0
        var framesLost = 0
        var falseLocks = 0

        val pipeline = ReceiverPipeline(
            band = profile.band,
            audioSpec = audioSpec,
            listener = { event ->
                events.add(event)
                when (event) {
                    is ReceiveEvent.Locked -> {
                        if (lockedSample == null) {
                            lockedSample = 0L // locked
                        }
                    }
                    is ReceiveEvent.FrameDecoded -> {
                        rsCorrections += event.correctedBytes
                    }
                    is ReceiveEvent.FrameFailed -> {
                        framesLost++
                        if (events.none { it is ReceiveEvent.ModeDecoded }) {
                            falseLocks++
                        }
                    }
                    else -> {}
                }
            },
            passphrase = passphrase
        )

        pipeline.push(impairedPcm)

        val completeEvent = events.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        val success = completeEvent != null
        val exactPayloadMatch = if (success) {
            completeEvent!!.message.payload.contentEquals(message.payload)
        } else false

        val timeToLockMs = if (lockedSample != null) {
            (plan.leadInSilenceMs + 16 * 30).toDouble() // preamble duration to lock
        } else null

        val ser = if (success && rsCorrections == 0) {
            0.0
        } else if (success) {
            (rsCorrections.toDouble() / (plan.frames.size * profile.rsProfile.n)).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        return LoopbackResult(
            success = success,
            exactPayloadMatch = exactPayloadMatch,
            timeToLockMs = timeToLockMs,
            symbolErrorRate = ser,
            totalRsCorrections = rsCorrections,
            framesLost = framesLost,
            falseLocks = falseLocks,
            durationSeconds = impairedPcm.size.toDouble() / audioSpec.sampleRateHz,
            receivedMessage = completeEvent?.message,
            receivedPayload = completeEvent?.payload
        )
    }

    /**
     * Convenience overload taking raw payload bytes and type.
     */
    public fun run(
        payload: ByteArray,
        type: MessageType = MessageType.TEXT,
        flags: MessageFlags = MessageFlags.NONE,
        profile: ModemProfile,
        scenario: ChannelScenario = ChannelScenario.CLEAN,
        audioSpec: AudioSpec = AudioSpec.RATE_48K,
        seed: Long = 42L,
        passphrase: String? = null
    ): LoopbackResult {
        val msg = OutgoingMessage(type = type, flags = flags, payload = payload, messageId = 42)
        return run(msg, profile, scenario, audioSpec, seed, passphrase)
    }
}
