package app.shoutsend.modem.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ByteUtilsTest {

    @Test
    fun bytesToHexCorrectness() {
        val bytes = byteArrayOf(0x00.toByte(), 0x29.toByte(), 0xB1.toByte(), 0xFF.toByte())
        assertEquals("0029b1ff", ByteUtils.bytesToHex(bytes))
    }

    @Test
    fun constantTimeEqualsCorrectness() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 5)
        val c = byteArrayOf(1, 2, 3, 4, 6)
        val d = byteArrayOf(1, 2, 3, 4)

        assertTrue(ByteUtils.constantTimeEquals(a, b))
        assertFalse(ByteUtils.constantTimeEquals(a, c))
        assertFalse(ByteUtils.constantTimeEquals(a, d))
    }
}
