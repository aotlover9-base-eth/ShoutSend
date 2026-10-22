package app.shoutsend.modem.testutil

import app.shoutsend.modem.synth.PcmConverter
import java.io.File
import java.io.FileOutputStream

/**
 * Test-only utility for writing standard 16-bit mono RIFF/WAVE files to disk.
 * Allows developers and owners to listen to synthesized transmissions using external players.
 */
public object WavWriter {

    /**
     * Writes 16-bit mono PCM samples to [file] at the given [sampleRateHz].
     */
    public fun writeWav(file: File, pcm16Samples: ShortArray, sampleRateHz: Int) {
        val pcmBytes = PcmConverter.toByteArray(pcm16Samples)
        val dataSize = pcmBytes.size
        val totalFileSize = 36 + dataSize

        file.parentFile?.mkdirs()

        FileOutputStream(file).use { fos ->
            val header = ByteArray(44)

            // 0..3: "RIFF"
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            // 4..7: Overall size - 8 bytes
            header[4] = (totalFileSize and 0xFF).toByte()
            header[5] = ((totalFileSize ushr 8) and 0xFF).toByte()
            header[6] = ((totalFileSize ushr 16) and 0xFF).toByte()
            header[7] = ((totalFileSize ushr 24) and 0xFF).toByte()

            // 8..11: "WAVE"
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // 12..15: "fmt "
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()

            // 16..19: Subchunk1Size = 16 for PCM
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            // 20..21: AudioFormat = 1 (PCM)
            header[20] = 1
            header[21] = 0

            // 22..23: NumChannels = 1 (Mono)
            header[22] = 1
            header[23] = 0

            // 24..27: SampleRate
            header[24] = (sampleRateHz and 0xFF).toByte()
            header[25] = ((sampleRateHz ushr 8) and 0xFF).toByte()
            header[26] = ((sampleRateHz ushr 16) and 0xFF).toByte()
            header[27] = ((sampleRateHz ushr 24) and 0xFF).toByte()

            // 28..31: ByteRate = SampleRate * NumChannels * (BitsPerSample / 8) = SampleRate * 2
            val byteRate = sampleRateHz * 2
            header[28] = (byteRate and 0xFF).toByte()
            header[29] = ((byteRate ushr 8) and 0xFF).toByte()
            header[30] = ((byteRate ushr 16) and 0xFF).toByte()
            header[31] = ((byteRate ushr 24) and 0xFF).toByte()

            // 32..33: BlockAlign = NumChannels * (BitsPerSample / 8) = 2
            header[32] = 2
            header[33] = 0

            // 34..35: BitsPerSample = 16
            header[34] = 16
            header[35] = 0

            // 36..39: "data"
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()

            // 40..43: Subchunk2Size = Data size in bytes
            header[40] = (dataSize and 0xFF).toByte()
            header[41] = ((dataSize ushr 8) and 0xFF).toByte()
            header[42] = ((dataSize ushr 16) and 0xFF).toByte()
            header[43] = ((dataSize ushr 24) and 0xFF).toByte()

            fos.write(header)
            fos.write(pcmBytes)
        }
    }
}
