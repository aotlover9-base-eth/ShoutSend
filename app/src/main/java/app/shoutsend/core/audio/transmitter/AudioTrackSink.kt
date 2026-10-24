package app.shoutsend.core.audio.transmitter

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import app.shoutsend.core.audio.AudioConfig
import kotlin.math.max

/**
 * Production implementation of [PcmSink] using Android's [AudioTrack] in streaming low-latency mode.
 */
class AudioTrackSink(
    val sampleRateHz: Int,
    bufferSizeInBytes: Int = defaultBufferSize(sampleRateHz)
) : PcmSink {

    private val audioTrack: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioConfig.ENCODING_PCM)
                .setSampleRate(sampleRateHz)
                .setChannelMask(AudioConfig.CHANNEL_CONFIG_OUT)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .setBufferSizeInBytes(bufferSizeInBytes)
        .build()

    override fun write(buffer: ShortArray, offset: Int, count: Int): Int {
        return audioTrack.write(buffer, offset, count, AudioTrack.WRITE_BLOCKING)
    }

    override fun play() {
        audioTrack.play()
    }

    override fun pause() {
        try {
            audioTrack.pause()
        } catch (_: Exception) {}
    }

    override fun flush() {
        try {
            audioTrack.flush()
        } catch (_: Exception) {}
    }

    override fun stop() {
        try {
            audioTrack.stop()
        } catch (_: Exception) {}
    }

    override fun release() {
        try {
            audioTrack.release()
        } catch (_: Exception) {}
    }

    override fun getPlaybackHeadPosition(): Long {
        return audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
    }

    override fun getUnderrunCount(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                audioTrack.underrunCount
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }
    }

    companion object {
        fun defaultBufferSize(sampleRateHz: Int): Int {
            val minSize = AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioConfig.CHANNEL_CONFIG_OUT,
                AudioConfig.ENCODING_PCM
            )
            val chunkBytes = AudioConfig.chunkSamples(sampleRateHz) * 2
            return max(minSize * 2, chunkBytes * 4)
        }

        fun selectSampleRate(): Int {
            val nativeRate = try {
                AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
            } catch (_: Exception) {
                48000
            }
            return if (nativeRate == 44100 || nativeRate == 48000) nativeRate else 48000
        }
    }
}
