package app.shoutsend.modem.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class Crc16Test {

    @Test
    fun knownAnswerTest123456789() {
        val asciiInput = "123456789".toByteArray(Charsets.US_ASCII)
        val crc = Crc16.calculate(asciiInput)
        // Mandatory known answer: 0x29B1 (10673)
        println("CRC Known Answer Test: '123456789' -> 0x${crc.toString(16).uppercase()}")
        assertEquals(0x29B1, crc)
    }

    @Test
    fun emptyInputGivesInitialValue() {
        val empty = ByteArray(0)
        assertEquals(0xFFFF, Crc16.calculate(empty))
    }

    @Test
    fun incrementalMatchesOneShot() {
        val testData = "The quick brown fox jumps over the lazy dog".toByteArray(Charsets.UTF_8)
        val oneShot = Crc16.calculate(testData)

        var incremental = Crc16.INITIAL_VALUE
        for (b in testData) {
            incremental = Crc16.update(incremental, b)
        }
        assertEquals(oneShot, incremental)

        // Split in two slices
        val mid = testData.size / 2
        var split = Crc16.INITIAL_VALUE
        split = Crc16.update(split, testData, 0, mid)
        split = Crc16.update(split, testData, mid, testData.size - mid)
        assertEquals(oneShot, split)
    }
}
