package app.shoutsend.modem.calibration

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.TonePlan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic audio calibration test sequence implementing [AudioStreamSource].
 *
 * Sequence structure:
 * 1. 1000 ms initial digital silence (for ambient noise floor profiling).
 * 2. Retro band test sequence:
 *    - Marker tone: 1000 Hz for 200 ms + 50 ms silence.
 *    - 16 data tones (1500 Hz .. 3375 Hz): 150 ms each + 50 ms silence.
 *    - 2 sync tones (S0: 1375 Hz, S1: 3500 Hz): 150 ms each + 50 ms silence.
 * 3. 500 ms inter-band transition silence.
 * 4. Stealth band test sequence:
 *    - Marker tone: 16500 Hz for 200 ms + 50 ms silence.
 *    - 16 data tones (17600 Hz .. 19850 Hz): 150 ms each + 50 ms silence.
 *    - 2 sync tones (S0: 17450 Hz, S1: 20000 Hz): 150 ms each + 50 ms silence.
 * 5. 500 ms tail silence.
 *
 * All tone bursts employ a 5 ms raised-cosine ramp to prevent acoustic clicks.
 * Rendering is thread-safe, deterministic, and strictly zero-allocation on the audio path.
 */
public class TestSequence(
    public override val audioSpec: AudioSpec = AudioSpec.RATE_48K,
    public val amplitude: Float = DEFAULT_AMPLITUDE
) : AudioStreamSource {

    public companion object {
        public const val DEFAULT_AMPLITUDE: Float = 0.80f
        public const val RETRO_MARKER_HZ: Double = 1000.0
        public const val STEALTH_MARKER_HZ: Double = 16500.0

        public const val INITIAL_SILENCE_MS: Int = 1000
        public const val MARKER_DURATION_MS: Int = 200
        public const val TONE_DURATION_MS: Int = 150
        public const val INTER_TONE_SILENCE_MS: Int = 50
        public const val INTER_BAND_SILENCE_MS: Int = 500
        public const val TAIL_SILENCE_MS: Int = 500
        public const val RAMP_DURATION_MS: Int = 5
    }

    private data class Segment(
        val startSample: Long,
        val endSample: Long,
        val frequencyHz: Double,
        val isTone: Boolean
    ) {
        val lengthSamples: Long = endSample - startSample
    }

    private val sampleRate: Int = audioSpec.sampleRateHz
    private val rampSamples: Int = (sampleRate * RAMP_DURATION_MS) / 1000
    private val segments: Array<Segment>

    public override val totalSamples: Long
    public override val durationSeconds: Double

    init {
        val segmentList = ArrayList<Segment>(80)
        var cursor: Long = 0L

        fun addSilence(ms: Int) {
            val count = (ms.toLong() * sampleRate) / 1000L
            if (count > 0) {
                segmentList.add(Segment(cursor, cursor + count, 0.0, isTone = false))
                cursor += count
            }
        }

        fun addTone(frequencyHz: Double, ms: Int) {
            val count = (ms.toLong() * sampleRate) / 1000L
            if (count > 0) {
                segmentList.add(Segment(cursor, cursor + count, frequencyHz, isTone = true))
                cursor += count
            }
        }

        // 1. Initial silence for ambient noise measurement
        addSilence(INITIAL_SILENCE_MS)

        // 2. Retro band sequence
        val retroPlan = TonePlan(Band.RETRO, audioSpec)
        addTone(RETRO_MARKER_HZ, MARKER_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)

        for (tone in retroPlan.dataTones) {
            addTone(tone, TONE_DURATION_MS)
            addSilence(INTER_TONE_SILENCE_MS)
        }
        addTone(retroPlan.syncTone0, TONE_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)
        addTone(retroPlan.syncTone1, TONE_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)

        // 3. Inter-band silence
        addSilence(INTER_BAND_SILENCE_MS)

        // 4. Stealth band sequence
        val stealthPlan = TonePlan(Band.STEALTH, audioSpec)
        addTone(STEALTH_MARKER_HZ, MARKER_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)

        for (tone in stealthPlan.dataTones) {
            addTone(tone, TONE_DURATION_MS)
            addSilence(INTER_TONE_SILENCE_MS)
        }
        addTone(stealthPlan.syncTone0, TONE_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)
        addTone(stealthPlan.syncTone1, TONE_DURATION_MS)
        addSilence(INTER_TONE_SILENCE_MS)

        // 5. Tail silence
        addSilence(TAIL_SILENCE_MS)

        segments = segmentList.toTypedArray()
        totalSamples = cursor
        durationSeconds = cursor.toDouble() / sampleRate.toDouble()
    }

    public override fun render(
        startSample: Long,
        out: FloatArray,
        outOffset: Int,
        count: Int
    ) {
        require(outOffset >= 0 && count >= 0 && outOffset + count <= out.size) { "Invalid bounds" }
        if (count == 0) return

        var segIdx = findSegmentIndex(startSample)

        for (i in 0 until count) {
            val sampleIdx = startSample + i
            if (sampleIdx < 0L || sampleIdx >= totalSamples) {
                out[outOffset + i] = 0.0f
                continue
            }

            while (segIdx < segments.size - 1 && sampleIdx >= segments[segIdx].endSample) {
                segIdx++
            }

            val seg = segments[segIdx]
            if (!seg.isTone || sampleIdx < seg.startSample || sampleIdx >= seg.endSample) {
                out[outOffset + i] = 0.0f
            } else {
                val n = (sampleIdx - seg.startSample).toInt()
                val segLen = seg.lengthSamples.toInt()

                // Calculate raised cosine ramp
                val ramp = when {
                    n < rampSamples -> {
                        0.5f * (1.0f - cos((PI * n) / rampSamples).toFloat())
                    }
                    n >= segLen - rampSamples -> {
                        0.5f * (1.0f + cos((PI * (n - (segLen - rampSamples))) / rampSamples).toFloat())
                    }
                    else -> 1.0f
                }

                val phase = (2.0 * PI * seg.frequencyHz * n) / sampleRate
                val sampleValue = (amplitude * ramp * sin(phase)).toFloat()
                out[outOffset + i] = sampleValue
            }
        }
    }

    private fun findSegmentIndex(sample: Long): Int {
        if (sample <= 0L) return 0
        var low = 0
        var high = segments.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val seg = segments[mid]
            if (sample < seg.startSample) {
                high = mid - 1
            } else if (sample >= seg.endSample) {
                low = mid + 1
            } else {
                return mid
            }
        }
        return low.coerceIn(0, segments.size - 1)
    }
}
