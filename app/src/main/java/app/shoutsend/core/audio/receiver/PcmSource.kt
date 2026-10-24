package app.shoutsend.core.audio.receiver

/**
 * Acoustic audio effects status for hardware echo cancellation, noise suppression, and AGC.
 */
data class AudioEffectsState(
    val aecAvailable: Boolean = false,
    val aecEnabled: Boolean = false,
    val nsAvailable: Boolean = false,
    val nsEnabled: Boolean = false,
    val agcAvailable: Boolean = false,
    val agcEnabled: Boolean = false
)

/**
 * Diagnostic source metadata describing the active capture hardware and configuration.
 */
data class SourceInfo(
    val sourceName: String,
    val sampleRateHz: Int,
    val channelCount: Int = 1,
    val effects: AudioEffectsState = AudioEffectsState()
)

/**
 * Abstraction for 16-bit linear PCM microphone capture.
 * Allows deterministic unit testing with fakes without requiring an Android audio server.
 */
interface PcmSource {
    /**
     * Prepares and starts hardware audio capture.
     *
     * @throws PermissionMissingException if microphone permission is not granted.
     * @throws InitFailedException if AudioRecord fails to initialize.
     * @throws UnsupportedRateException if neither 48 kHz nor 44.1 kHz is supported.
     */
    fun start()

    /**
     * Reads up to [count] 16-bit PCM frames into [buffer] starting at [offset].
     * Returns the number of frames read, or a negative error code.
     */
    fun read(buffer: ShortArray, offset: Int, count: Int): Int

    /**
     * Stops recording.
     */
    fun stop()

    /**
     * Releases hardware resources.
     */
    fun release()

    val actualSampleRate: Int
    val sourceInfo: SourceInfo
}

class PermissionMissingException(message: String = "RECORD_AUDIO permission is not granted") : Exception(message)
class InitFailedException(message: String) : Exception(message)
class UnsupportedRateException(message: String) : Exception(message)
class NoMicrophoneException(message: String = "No audio input hardware found on this device") : Exception(message)
