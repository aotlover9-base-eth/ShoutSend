package app.shoutsend.core.audio.receiver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import app.shoutsend.core.audio.AudioConfig
import kotlin.math.max

/**
 * Production implementation of [PcmSource] using Android's [AudioRecord].
 *
 * Configures the least processed audio source available, disables OS voice processing
 * effects (AEC, NS, AGC) to prevent modem tone distortion, and validates hardware state.
 */
class AudioRecordSource(
    private val context: Context,
    private val audioSourceOverride: Int? = null
) : PcmSource {

    private var audioRecord: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private var resolvedSampleRate: Int = 48000
    private var activeSourceInfo: SourceInfo = SourceInfo("UNINITIALIZED", resolvedSampleRate)

    override val actualSampleRate: Int
        get() = resolvedSampleRate

    override val sourceInfo: SourceInfo
        get() = activeSourceInfo

    override fun start() {
        if (audioRecord != null) return

        // 1. Verify runtime permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            throw PermissionMissingException()
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // 2. Determine best audio source
        val chosenSource = audioSourceOverride ?: determineBestAudioSource(audioManager)
        val sourceName = audioSourceName(chosenSource)

        // 3. Try sample rates: 48000 Hz, fallback to 44100 Hz
        val rates = intArrayOf(48000, 44100)
        var initializedRecord: AudioRecord? = null
        var chosenRate = 48000

        for (rate in rates) {
            val minBuf = AudioRecord.getMinBufferSize(
                rate,
                AudioConfig.CHANNEL_CONFIG_IN,
                AudioConfig.ENCODING_PCM
            )
            if (minBuf <= 0) continue

            val bufSize = max(minBuf, rate * 2 / 10) // At least 200ms buffer
            val record = try {
                AudioRecord(
                    chosenSource,
                    rate,
                    AudioConfig.CHANNEL_CONFIG_IN,
                    AudioConfig.ENCODING_PCM,
                    bufSize
                )
            } catch (e: SecurityException) {
                throw PermissionMissingException(e.message ?: "Microphone permission rejected")
            } catch (_: Exception) {
                null
            }

            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                initializedRecord = record
                chosenRate = rate
                break
            } else {
                record?.release()
            }
        }

        if (initializedRecord == null) {
            throw UnsupportedRateException("Failed to initialize AudioRecord at 48000 Hz or 44100 Hz")
        }

        audioRecord = initializedRecord
        resolvedSampleRate = chosenRate

        // 4. Disable hardware acoustic effects to protect acoustic data carrier
        val effectsState = configureAudioEffects(initializedRecord.audioSessionId)
        activeSourceInfo = SourceInfo(
            sourceName = sourceName,
            sampleRateHz = chosenRate,
            channelCount = 1,
            effects = effectsState
        )

        try {
            initializedRecord.startRecording()
        } catch (e: Exception) {
            release()
            throw InitFailedException("AudioRecord.startRecording() failed: ${e.message}")
        }
    }

    override fun read(buffer: ShortArray, offset: Int, count: Int): Int {
        val record = audioRecord ?: return -1
        return record.read(buffer, offset, count)
    }

    override fun stop() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
    }

    override fun release() {
        stop()
        try {
            aec?.release()
        } catch (_: Exception) {}
        aec = null

        try {
            ns?.release()
        } catch (_: Exception) {}
        ns = null

        try {
            agc?.release()
        } catch (_: Exception) {}
        agc = null

        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    private fun determineBestAudioSource(audioManager: AudioManager?): Int {
        val supportUnprocessed = audioManager?.getProperty(
            AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED
        )?.equals("true", ignoreCase = true) == true

        return when {
            supportUnprocessed -> MediaRecorder.AudioSource.UNPROCESSED
            else -> MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }

    private fun configureAudioEffects(audioSessionId: Int): AudioEffectsState {
        var aecAvail = false
        var aecActive = false
        var nsAvail = false
        var nsActive = false
        var agcAvail = false
        var agcActive = false

        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aecAvail = true
                aec = AcousticEchoCanceler.create(audioSessionId)?.apply {
                    enabled = false
                    aecActive = enabled
                }
            }
        } catch (_: Exception) {}

        try {
            if (NoiseSuppressor.isAvailable()) {
                nsAvail = true
                ns = NoiseSuppressor.create(audioSessionId)?.apply {
                    enabled = false
                    nsActive = enabled
                }
            }
        } catch (_: Exception) {}

        try {
            if (AutomaticGainControl.isAvailable()) {
                agcAvail = true
                agc = AutomaticGainControl.create(audioSessionId)?.apply {
                    enabled = false
                    agcActive = enabled
                }
            }
        } catch (_: Exception) {}

        return AudioEffectsState(
            aecAvailable = aecAvail,
            aecEnabled = aecActive,
            nsAvailable = nsAvail,
            nsEnabled = nsActive,
            agcAvailable = agcAvail,
            agcEnabled = agcActive
        )
    }

    private fun audioSourceName(source: Int): String = when (source) {
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        else -> "SOURCE_$source"
    }
}
