package app.shoutsend.core.audio.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SpscRingBufferTest {

    @Test
    fun testBasicWriteAndReadInOrder() {
        val ring = SpscRingBuffer(capacity = 16)
        val data = shortArrayOf(1, 2, 3, 4, 5)

        val written = ring.write(data, 0, 5)
        assertEquals(5, written)
        assertEquals(5, ring.availableSamples)
        assertEquals(11, ring.availableCapacity)

        val out = ShortArray(5)
        val read = ring.read(out, 0, 5)
        assertEquals(5, read)
        for (i in 0 until 5) {
            assertEquals((i + 1).toShort(), out[i])
        }
        assertEquals(0, ring.availableSamples)
    }

    @Test
    fun testOverrunDropAccounting() {
        val ring = SpscRingBuffer(capacity = 8)
        val data = shortArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

        // Capacity is 8, attempting to write 10 samples
        val written = ring.write(data, 0, 10)
        assertEquals(8, written)
        assertEquals(2L, ring.overrunCount)
        assertEquals(8, ring.availableSamples)

        val out = ShortArray(8)
        val read = ring.read(out, 0, 8)
        assertEquals(8, read)
        assertEquals(10.toShort(), out[0])
        assertEquals(80.toShort(), out[7])
    }

    @Test
    fun testConcurrentProducerConsumerIntegrity() {
        val capacity = 1024
        val ring = SpscRingBuffer(capacity = capacity)
        val totalSamples = 100_000

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)
        val failed = AtomicBoolean(false)

        // Producer thread
        val producer = Thread {
            startLatch.await()
            val chunk = ShortArray(64)
            var currentVal = 0
            while (currentVal < totalSamples) {
                val toSend = minOf(64, totalSamples - currentVal)
                for (i in 0 until toSend) {
                    chunk[i] = (currentVal + i).toShort()
                }
                while (ring.availableCapacity < toSend) {
                    Thread.yield()
                }
                val w = ring.write(chunk, 0, toSend)
                assertEquals(toSend, w)
                currentVal += toSend
            }
            doneLatch.countDown()
        }

        // Consumer thread
        val consumer = Thread {
            startLatch.await()
            val out = ShortArray(64)
            var expectedVal = 0
            while (expectedVal < totalSamples) {
                val r = ring.read(out, 0, 64)
                if (r > 0) {
                    for (i in 0 until r) {
                        val expected = (expectedVal + i).toShort()
                        if (out[i] != expected) {
                            failed.set(true)
                            break
                        }
                    }
                    expectedVal += r
                } else {
                    Thread.yield()
                }
            }
            doneLatch.countDown()
        }

        producer.start()
        consumer.start()
        startLatch.countDown()

        val finished = doneLatch.await(5, TimeUnit.SECONDS)
        assertTrue("Producer and consumer completed within timeout", finished)
        assertTrue("No sample corruption detected during concurrent transfer", !failed.get())
        assertEquals("Zero overruns when consumer keeps pace", 0L, ring.overrunCount)
    }
}
