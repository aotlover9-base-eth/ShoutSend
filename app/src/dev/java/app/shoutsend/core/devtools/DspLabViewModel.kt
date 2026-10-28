package app.shoutsend.core.devtools

import androidx.lifecycle.ViewModel
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.detector.GoertzelBank
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

/**
 * UI state for the DSP Lab diagnostic tool.
 */
data class DspLabUiState(
    val band: Band = Band.RETRO,
    val speed: Speed = Speed.NORMAL,
    val toneFrequencies: List<Double> = emptyList(),
    val binPowers: FloatArray = FloatArray(16),
    val activeToneIndex: Int = -1,
    val detectedToneIndex: Int = -1,
    val peakFrequencyHz: Double = 0.0,
    val snrDb: Double = 0.0,
    val noiseFloorDbfs: Float = -60.0f,
    val isInjectingNoise: Boolean = false,
    val statusMessage: String = "Ready for synthetic tone injection"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DspLabUiState) return false
        return band == other.band &&
            speed == other.speed &&
            toneFrequencies == other.toneFrequencies &&
            binPowers.contentEquals(other.binPowers) &&
            activeToneIndex == other.activeToneIndex &&
            detectedToneIndex == other.detectedToneIndex &&
            peakFrequencyHz == other.peakFrequencyHz &&
            snrDb == other.snrDb &&
            noiseFloorDbfs == other.noiseFloorDbfs &&
            isInjectingNoise == other.isInjectingNoise &&
            statusMessage == other.statusMessage
    }

    override fun hashCode(): Int {
        var result = band.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + toneFrequencies.hashCode()
        result = 31 * result + binPowers.contentHashCode()
        result = 31 * result + activeToneIndex
        result = 31 * result + detectedToneIndex
        result = 31 * result + peakFrequencyHz.hashCode()
        result = 31 * result + snrDb.hashCode()
        result = 31 * result + noiseFloorDbfs.hashCode()
        result = 31 * result + isInjectingNoise.hashCode()
        result = 31 * result + statusMessage.hashCode()
        return result
    }
}

/**
 * ViewModel managing the DSP Lab diagnostic playground (dev flavor only).
 *
 * Allows real-time synthetic MFSK tone generation, Goertzel filter inspect,
 * noise injection, and SNR calculation.
 */
class DspLabViewModel(
    initialBand: Band = Band.RETRO,
    initialSpeed: Speed = Speed.NORMAL,
    private val audioSpec: AudioSpec = AudioSpec(sampleRateHz = 48000)
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        createInitialState(initialBand, initialSpeed)
    )
    val uiState: StateFlow<DspLabUiState> = _uiState.asStateFlow()

    private fun createInitialState(band: Band, speed: Speed): DspLabUiState {
        val plan = TonePlan(band, audioSpec)
        return DspLabUiState(
            band = band,
            speed = speed,
            toneFrequencies = plan.dataTones
        )
    }

    fun selectBand(band: Band) {
        val plan = TonePlan(band, audioSpec)
        _uiState.update {
            it.copy(
                band = band,
                toneFrequencies = plan.dataTones,
                binPowers = FloatArray(16),
                activeToneIndex = -1,
                detectedToneIndex = -1,
                peakFrequencyHz = 0.0,
                snrDb = 0.0,
                statusMessage = "Switched to ${band.name} band"
            )
        }
    }

    fun selectSpeed(speed: Speed) {
        _uiState.update {
            it.copy(
                speed = speed,
                statusMessage = "Switched to ${speed.name} speed"
            )
        }
    }

    fun toggleNoiseInjection() {
        _uiState.update {
            val newState = !it.isInjectingNoise
            it.copy(
                isInjectingNoise = newState,
                statusMessage = if (newState) "Noise injection enabled (+12 dB floor)" else "Noise injection disabled"
            )
        }
    }

    fun injectTone(toneIndex: Int) {
        require(toneIndex in 0..15) { "Tone index must be in 0..15: $toneIndex" }
        val current = _uiState.value
        val plan = TonePlan(current.band, audioSpec)
        val targetFreq = plan.dataTones[toneIndex]
        val symbolSamples = Timing.symbolSamples(current.speed, audioSpec)

        // Synthesize single tone sine burst
        val samples = FloatArray(symbolSamples)
        val noiseAmp = if (current.isInjectingNoise) 0.15f else 0.005f
        for (i in 0 until symbolSamples) {
            val signal = 0.8f * sin((2.0 * PI * targetFreq * i) / audioSpec.sampleRateHz.toDouble()).toFloat()
            // Simple pseudo-noise pseudo-random component
            val noise = (sin(12345.67 * (i + 1)) * noiseAmp).toFloat()
            samples[i] = signal + noise
        }

        // Run Goertzel detector bank across all 16 data tone frequencies
        val detector = GoertzelBank(
            frequencies = plan.dataTones.toDoubleArray(),
            audioSpec = audioSpec,
            blockSize = symbolSamples,
            windowed = true
        )
        val powers = FloatArray(16)
        detector.energies(samples, 0, powers)

        // Find detected tone and SNR
        var maxPower = -1.0f
        var maxIdx = -1
        var sumNoisePower = 0.0f
        for (i in 0 until 16) {
            if (powers[i] > maxPower) {
                maxPower = powers[i]
                maxIdx = i
            }
            if (i != toneIndex) {
                sumNoisePower += powers[i]
            }
        }
        val avgNoise = max(sumNoisePower / 15.0f, 1e-8f)
        val snr = 10.0 * log10((maxPower / avgNoise).toDouble().coerceAtLeast(1.0))
        val noiseDbfs = 10.0f * log10(avgNoise.coerceAtLeast(1e-9f))

        _uiState.update {
            it.copy(
                binPowers = powers,
                activeToneIndex = toneIndex,
                detectedToneIndex = maxIdx,
                peakFrequencyHz = targetFreq,
                snrDb = snr,
                noiseFloorDbfs = noiseDbfs,
                statusMessage = "Injected Tone $toneIndex (${targetFreq.toInt()} Hz) -> Detected Tone $maxIdx"
            )
        }
    }

    fun clear() {
        _uiState.update {
            it.copy(
                binPowers = FloatArray(16),
                activeToneIndex = -1,
                detectedToneIndex = -1,
                peakFrequencyHz = 0.0,
                snrDb = 0.0,
                statusMessage = "Cleared"
            )
        }
    }
}
