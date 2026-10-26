package app.shoutsend.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.shoutsend.core.data.settings.SettingsRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Low-latency audio feedback player for subtle acoustic earcons.
 */
interface AudioFeedbackPlayer {
    /** Plays an upward chirp sweep earcon for transmission initiation. */
    fun playTransmitStart()

    /** Plays a subtle acoustic click earcon on preamble sync lock. */
    fun playReceiveLocked()

    /** Plays an ascending harmonic chord chime on transfer or frame completion. */
    fun playSuccess()

    /** Plays a subtle low tone earcon on transmission or decode failure. */
    fun playError()

    /** Plays the requested [Earcon]. */
    fun play(earcon: Earcon)

    /** Releases all hardware audio track resources. */
    fun release()
}

/**
 * No-op implementation of [AudioFeedbackPlayer] for testing and previews.
 */
object NoOpAudioFeedbackPlayer : AudioFeedbackPlayer {
    override fun playTransmitStart() {}
    override fun playReceiveLocked() {}
    override fun playSuccess() {}
    override fun playError() {}
    override fun play(earcon: Earcon) {}
    override fun release() {}
}

/**
 * Abstraction for underlying hardware PCM audio sink to permit pure JVM testing.
 */
interface AudioEarconSink {
    fun play(earcon: Earcon, pcm: ShortArray)
    fun release()
}

/**
 * Production Android [AudioEarconSink] using static [AudioTrack] instances for minimal latency.
 */
class DefaultAudioEarconSink(
    private val sampleRateHz: Int = EarconSynthesizer.DEFAULT_SAMPLE_RATE_HZ
) : AudioEarconSink {

    private val tracks = ConcurrentHashMap<Earcon, AudioTrack>()
    private val lock = Any()
    @Volatile private var isReleased = false

    private fun getOrCreateTrack(earcon: Earcon, pcm: ShortArray): AudioTrack? {
        if (isReleased) return null
        val existing = tracks[earcon]
        if (existing != null) return existing
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRateHz)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            tracks[earcon] = track
            track
        } catch (_: Exception) {
            null
        }
    }

    override fun play(earcon: Earcon, pcm: ShortArray) {
        if (isReleased) return
        synchronized(lock) {
            try {
                val track = getOrCreateTrack(earcon, pcm) ?: return
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
            } catch (_: Exception) {
                // Non-fatal if audio service is transiently unavailable
            }
        }
    }

    override fun release() {
        isReleased = true
        synchronized(lock) {
            for ((_, track) in tracks) {
                try {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.release()
                } catch (_: Exception) {}
            }
            tracks.clear()
        }
    }
}

/**
 * Production implementation of [AudioFeedbackPlayer].
 *
 * Checks user preferences in [settingsRepository] and validates device ringer mode,
 * mute status, and audio communication focus before emitting sounds.
 */
class DefaultAudioFeedbackPlayer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
    private val audioSink: AudioEarconSink = DefaultAudioEarconSink(sampleRateHz = EarconSynthesizer.DEFAULT_SAMPLE_RATE_HZ)
) : AudioFeedbackPlayer {

    private fun isAudible(): Boolean {
        // 1. Respect user settings switch
        if (!settingsRepository.settings.value.soundEffectsEnabled) {
            return false
        }

        val am = audioManager ?: return true

        // 2. Respect silent / vibrate ringer mode
        val ringer = am.ringerMode
        if (ringer == AudioManager.RINGER_MODE_SILENT || ringer == AudioManager.RINGER_MODE_VIBRATE) {
            return false
        }

        // 3. Do not interrupt ongoing voice calls or communications
        val mode = am.mode
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
            return false
        }

        // 4. Respect hardware volume levels (muted notification/system/music)
        val musicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val sysVol = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
        val notifVol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        if (musicVol == 0 && sysVol == 0 && notifVol == 0) {
            return false
        }

        return true
    }

    override fun play(earcon: Earcon) {
        if (!isAudible()) return
        val pcm = when (earcon) {
            Earcon.TRANSMIT_START -> EarconSynthesizer.transmitStartPcm
            Earcon.RECEIVE_LOCKED -> EarconSynthesizer.receiveLockedPcm
            Earcon.SUCCESS -> EarconSynthesizer.successPcm
            Earcon.ERROR -> EarconSynthesizer.errorPcm
        }
        audioSink.play(earcon, pcm)
    }

    override fun playTransmitStart() = play(Earcon.TRANSMIT_START)
    override fun playReceiveLocked() = play(Earcon.RECEIVE_LOCKED)
    override fun playSuccess() = play(Earcon.SUCCESS)
    override fun playError() = play(Earcon.ERROR)

    override fun release() {
        audioSink.release()
    }
}
