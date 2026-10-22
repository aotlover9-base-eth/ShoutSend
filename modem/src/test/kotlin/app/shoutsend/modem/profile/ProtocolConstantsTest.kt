package app.shoutsend.modem.profile

import app.shoutsend.modem.protocol.ProtocolConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolConstantsTest {

    @Test
    fun preambleAlternatesS0AndS1() {
        val pattern = ProtocolConstants.PREAMBLE_PATTERN
        assertEquals(16, pattern.size)
        for (i in pattern.indices) {
            if (i % 2 == 0) {
                assertEquals(ProtocolConstants.SYNC_TONE_0_INDEX, pattern[i])
            } else {
                assertEquals(ProtocolConstants.SYNC_TONE_1_INDEX, pattern[i])
            }
        }
    }

    @Test
    fun syncWordAutocorrelationSideLobesAreZero() {
        val syncWord = ProtocolConstants.SYNC_WORD_PATTERN
        assertEquals(8, syncWord.size)

        // Check aperiodic autocorrelation match count
        for (lag in -(syncWord.size - 1) until syncWord.size) {
            var matches = 0
            for (i in syncWord.indices) {
                val j = i + lag
                if (j in syncWord.indices && syncWord[i] == syncWord[j]) {
                    matches++
                }
            }
            if (lag == 0) {
                assertEquals("Lag 0 must match all symbols", syncWord.size, matches)
            } else {
                assertEquals("Lag $lag must have 0 side-lobe matches", 0, matches)
            }
        }
    }

    @Test
    fun frameMarkerDoesNotOverlapWithSyncWord() {
        val syncWord = ProtocolConstants.SYNC_WORD_PATTERN.toSet()
        val marker = ProtocolConstants.FRAME_MARKER_PATTERN.toSet()
        val intersection = syncWord.intersect(marker)
        assertTrue("Sync word and frame marker must not share tone indices: $intersection", intersection.isEmpty())
    }
}
