package app.shoutsend.core.capability

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import app.shoutsend.core.audio.AudioConfig
import app.shoutsend.core.audio.receiver.AudioEffectsState
import app.shoutsend.core.audio.transmitter.OutputRouteInfo
import app.shoutsend.core.audio.transmitter.getOutputRouteInfo

/**
 * Inspection engine querying Android audio system properties, features, and hardware limits.
 */
class CapabilityProbe(
    private val context: Context,
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
) {

    /**
     * Inspects hardware state and returns a complete [CapabilityReport].
     */
    fun probe(): CapabilityReport {
        val nativeRateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val nativeRate = nativeRateStr?.toIntOrNull() ?: 48000

        val framesStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val framesPerBuffer = framesStr?.toIntOrNull() ?: 256

        // Query supported input sample rates
        val supportedRates = ArrayList<Int>(2)
        for (rate in AudioConfig.PREFERRED_SAMPLE_RATES) {
            val minBuf = try {
                AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            } catch (_: Exception) {
                -1
            }
            if (minBuf > 0) {
                supportedRates.add(rate)
            }
        }
        if (supportedRates.isEmpty()) {
            supportedRates.add(48000)
        }

        val unprocessedSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        } else false

        val pm = context.packageManager
        val hasLowLatency = pm?.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY) ?: false
        val hasProAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO) ?: false
        } else false

        val aecAvailable = try { AcousticEchoCanceler.isAvailable() } catch (_: Throwable) { false }
        val nsAvailable = try { NoiseSuppressor.isAvailable() } catch (_: Throwable) { false }
        val agcAvailable = try { AutomaticGainControl.isAvailable() } catch (_: Throwable) { false }

        val effectsState = AudioEffectsState(
            aecAvailable = aecAvailable,
            aecEnabled = false,
            nsAvailable = nsAvailable,
            nsEnabled = false,
            agcAvailable = agcAvailable,
            agcEnabled = false
        )

        val outputRoute = getOutputRouteInfo(context)
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return CapabilityReport(
            nativeOutputSampleRateHz = nativeRate,
            outputFramesPerBuffer = framesPerBuffer,
            supportedInputSampleRates = supportedRates,
            isUnprocessedSupported = unprocessedSupported,
            hasLowLatencyAudio = hasLowLatency,
            hasProAudio = hasProAudio,
            deviceModel = if (deviceModel.isEmpty()) "Android Device" else deviceModel,
            effects = effectsState,
            outputRoute = outputRoute
        )
    }
}
