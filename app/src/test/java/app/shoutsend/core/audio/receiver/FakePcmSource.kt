package app.shoutsend.core.audio.receiver

import kotlin.math.min

/**
 * Test fake for [PcmSource] delivering canned audio samples in configurable chunk sizes.
 */
class FakePcmSource(
    private val audioData: ShortArray = ShortArray(0),
    override val actualSampleRate: Int = 48000,
    override val sourceInfo: SourceInfo = SourceInfo(
        sourceName = "FAKE_UNPROCESSED",
        sampleRateHz = actualSampleRate,
        channelCount = 1,
        effects = AudioEffectsState(aecAvailable = true, aecEnabled = false)
    )
) : PcmSource {

    var isStarted: Boolean = false
        private set
    var isStopped: Boolean = false
        private set
    var isReleased: Boolean = false
        private set

    var totalSamplesRead: Long = 0L
        private set

    private var cursor: Int = 0

    override fun start() {
        isStarted = true
        isStopped = false
        isReleased = false
        cursor = 0
    }

    override fun read(buffer: ShortArray, offset: Int, count: Int): Int {
        if (!isStarted || isStopped || isReleased) return -1
        if (cursor >= audioData.size) {
            // Emulate silence past the end with slight sleep to pace the reader thread
            try {
                Thread.sleep(2)
            } catch (_: InterruptedException) {
                return -1
            }
            buffer.fill(0, offset, offset + count)
            totalSamplesRead += count
            return count
        }

        val available = audioData.size - cursor
        val toCopy = min(count, available)
        System.arraycopy(audioData, cursor, buffer, offset, toCopy)
        cursor += toCopy

        // Pad remainder with zeros if audioData exhausted mid-chunk
        if (toCopy < count) {
            buffer.fill(0, offset + toCopy, offset + count)
        }

        // Slight sleep to emulate hardware audio pacing and prevent consumer overrun
        try {
            Thread.sleep(1)
        } catch (_: InterruptedException) {
            return -1
        }

        totalSamplesRead += count
        return count
    }

    override fun stop() {
        isStopped = true
    }

    override fun release() {
        isReleased = true
        isStopped = true
        isStarted = false
    }
}
