package app.shoutsend.modem.protocol

import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BitStreamTest {

    private val rng = SeededRandom(101L)

    @Test
    fun roundTripRandomBitWidths() {
        val writer = BitWriter()
        val expectedValues = mutableListOf<Pair<Int, Int>>() // Pair(value, numBits)

        var totalBits = 0
        for (i in 0 until 100) {
            val numBits = rng.nextInt(1, 25)
            val mask = if (numBits == 32) -1 else (1 shl numBits) - 1
            val value = rng.nextInt(0, Int.MAX_VALUE) and mask

            writer.writeBits(value, numBits)
            expectedValues.add(Pair(value, numBits))
            totalBits += numBits
        }

        assertEquals(totalBits, writer.totalBits)

        val bytes = writer.toByteArray()
        val reader = BitReader(bytes, totalBits)

        for ((expectedValue, numBits) in expectedValues) {
            val readValue = reader.readBits(numBits)
            assertEquals("Mismatch for $numBits-bit value", expectedValue, readValue)
        }

        assertEquals(0, reader.remainingBits)
        assertFalse(reader.hasRemaining)
    }

    @Test
    fun throwsOnReadPastEnd() {
        val writer = BitWriter()
        writer.writeBits(0x05, 3) // 3 bits

        val reader = BitReader(writer.toByteArray(), 3)
        assertEquals(3, reader.remainingBits)
        val value = reader.readBits(3)
        assertEquals(0x05, value)

        assertThrows(BitStreamException::class.java) {
            reader.readBits(1)
        }
    }
}
