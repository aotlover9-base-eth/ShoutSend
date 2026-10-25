package app.shoutsend.core.session

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Controller abstraction managing audio focus and detecting external media playback.
 */
interface AudioFocusController {
    /** Whether another application is currently playing music or audio. */
    val isMusicActive: Boolean

    /**
     * Requests transient audio focus (`AUDIOFOCUS_GAIN_TRANSIENT`) for acoustic transmission.
     *
     * @param onFocusLost Callback invoked if focus is revoked during transmission (e.g. phone call).
     * @return True if focus was granted, false if rejected.
     */
    fun requestFocus(onFocusLost: (reason: String) -> Unit): Boolean

    /**
     * Releases audio focus back to previous media players. Idempotent.
     */
    fun abandonFocus()
}

/**
 * Production Android implementation of [AudioFocusController] using [AudioManager].
 */
class DefaultAudioFocusController(
    private val context: Context,
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
) : AudioFocusController {

    private var activeFocusRequest: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null

    override val isMusicActive: Boolean
        get() = audioManager?.isMusicActive ?: false

    @Synchronized
    override fun requestFocus(onFocusLost: (reason: String) -> Unit): Boolean {
        if (audioManager == null) return true

        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    onFocusLost("AudioFocusLost")
                }
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            activeFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            legacyListener = focusChangeListener
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    @Synchronized
    override fun abandonFocus() {
        if (audioManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeFocusRequest?.let { req ->
                audioManager.abandonAudioFocusRequest(req)
            }
            activeFocusRequest = null
        } else {
            legacyListener?.let { l ->
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(l)
            }
            legacyListener = null
        }
    }
}
