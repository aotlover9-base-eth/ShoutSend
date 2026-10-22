package app.shoutsend.modem.modulator

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.packet.Frame
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.synth.SymbolSchedule

/**
 * Complete acoustic transmission blueprint ready for deterministic streaming audio modulation.
 *
 * @property message The source outgoing message.
 * @property frames Ordered list of fragmented physical frames.
 * @property codewords Systematic Reed-Solomon codewords corresponding to each frame.
 * @property schedule Ordered timeline of physical acoustic symbol events.
 * @property profile Modem configuration specifying band, scheme, speed, and RS profile.
 * @property tonePlan Physical frequency grid for symbol tone indexes.
 * @property audioSpec Sample rate specification (48 kHz or 44.1 kHz).
 * @property leadInSilenceMs Hardware warm-up lead-in silence in milliseconds.
 * @property tailSilenceMs Hardware cooldown tail silence in milliseconds.
 * @property totalSamples Total audio duration in samples including lead-in and tail silence.
 */
public class TransmissionPlan(
    public val message: OutgoingMessage,
    public val frames: List<Frame>,
    public val codewords: List<ByteArray>,
    public val schedule: SymbolSchedule,
    public val profile: ModemProfile,
    public val tonePlan: TonePlan,
    public val audioSpec: AudioSpec,
    public val leadInSilenceMs: Int,
    public val tailSilenceMs: Int,
    public val totalSamples: Long
) {
    /**
     * Total transmission duration in fractional seconds.
     */
    public val durationSeconds: Double = totalSamples.toDouble() / audioSpec.sampleRateHz
}
