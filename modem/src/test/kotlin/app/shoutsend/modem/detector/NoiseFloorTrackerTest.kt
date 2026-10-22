package app.shoutsend.modem.detector

import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

class NoiseFloorTrackerTest {

    @Test
    fun testWhiteNoiseProducesStableFloorsWithLowVariance() {
        val toneCount = 16
        val params = NoiseTrackerParams(hopIntervalSec = 0.010) // 10ms hop
        val tracker = NoiseFloorTracker(toneCount, params)

        val rng = SeededRandom(12345L)
        val noiseEnergies = FloatArray(toneCount)
        val targetPower = 1e-4f // -40 dBFS

        // Feed 1000 hops (10 seconds) of stationary noise with random fluctuation
        for (hop in 0 until 1000) {
            for (t in 0 until toneCount) {
                // Fluctuating noise energy around targetPower
                val factor = (0.7 + 0.6 * rng.nextDouble()).toFloat()
                noiseEnergies[t] = targetPower * factor
            }
            tracker.update(noiseEnergies)
        }

        // Check variance across tones: tracked floors should be uniform and stable
        var sum = 0.0
        for (t in 0 until toneCount) {
            sum += tracker.noiseFloor(t)
        }
        val meanFloor = (sum / toneCount).toFloat()

        var sumSqDiff = 0.0
        for (t in 0 until toneCount) {
            val diff = tracker.noiseFloor(t) - meanFloor
            sumSqDiff += diff * diff
        }
        val variance = sumSqDiff / toneCount
        val stdDev = kotlin.math.sqrt(variance).toFloat()
        val relativeStdDev = stdDev / meanFloor

        println("Noise floor across $toneCount tones: mean = $meanFloor, stdDev = $stdDev, relativeStdDev = ${String.format("%.4f", relativeStdDev)}")
        // Assert relative standard deviation across tones is strictly bounded (< 3%)
        assertTrue("Tone-to-tone standard deviation ($relativeStdDev) must be < 0.03", relativeStdDev < 0.03f)
    }

    @Test
    fun testStepChangeAdaptationTimes() {
        // Measure adaptation time (in seconds) to reach within 1.0 dB of new target level
        val stepDbs = doubleArrayOf(20.0, 10.0, -10.0, -20.0)
        val adaptationTimesSec = DoubleArray(stepDbs.size)

        val hopSec = 0.010
        val basePower = 1e-4f // -40 dBFS

        println("\n=== Noise Floor Step Adaptation Times ===")
        println("| Step (dB) | Direction | Target Power | Target dBFS | Time to Within 1 dB |")
        println("| :---      | :---      | :---         | :---        | :---                |")

        for (i in stepDbs.indices) {
            val stepDb = stepDbs[i]
            val targetPower = (basePower * 10.0.pow(stepDb / 10.0)).toFloat()
            val targetDbfs = 10.0 * log10(targetPower.toDouble())

            val tracker = NoiseFloorTracker(1, NoiseTrackerParams(hopIntervalSec = hopSec, initialFloorPower = basePower))
            val energies = floatArrayOf(targetPower)

            var hops = 0
            var converged = false
            while (hops < 3000) { // Max 30 seconds
                tracker.update(energies)
                hops++

                val currentFloor = tracker.noiseFloor(0)
                val currentDb = 10.0 * log10(currentFloor.toDouble())
                if (abs(currentDb - targetDbfs) <= 1.0) {
                    converged = true
                    break
                }
            }

            assertTrue("Step $stepDb dB must converge within 30s", converged)
            val timeSec = hops * hopSec
            adaptationTimesSec[i] = timeSec

            val dir = if (stepDb > 0) "Rise (Slow)" else "Fall (Fast)"
            println(
                "| ${if (stepDb > 0) "+" else ""}${stepDb.toInt()} dB | $dir | ${String.format("%.2e", targetPower)} | " +
                        "${String.format("%.1f", targetDbfs)} dBFS | ${String.format("%.3f", timeSec)}s |"
            )
        }

        // Due to asymmetric time constants (decay 0.25s vs attack 1.5s):
        // Downward adaptation (-10 dB) must be substantially faster than upward (+10 dB)
        val timeUp10 = adaptationTimesSec[1]
        val timeDown10 = adaptationTimesSec[2]
        assertTrue("Downward step (-10 dB, ${timeDown10}s) must be faster than upward (+10 dB, ${timeUp10}s)", timeDown10 < timeUp10)
    }

    @Test
    fun testFreezePreventsSignalFromContaminatingNoiseFloor() {
        val tracker = NoiseFloorTracker(2, NoiseTrackerParams(hopIntervalSec = 0.010, initialFloorPower = 1e-4f))
        val initialFloor = tracker.noiseFloor(0)

        // 1. When FROZEN, 3-second high-power tone burst (300 hops @ 0.64 power) does not move floor
        tracker.freeze()
        assertTrue(tracker.isFrozen())

        val loudSignal = floatArrayOf(0.64f, 0.64f)
        for (i in 0 until 300) {
            tracker.update(loudSignal)
        }
        assertEquals("Frozen tracker must not move floor at all", initialFloor, tracker.noiseFloor(0), 1e-8f)

        // 2. When UNFROZEN, the same tone burst moves the floor upward significantly
        tracker.unfreeze()
        assertFalse(tracker.isFrozen())
        for (i in 0 until 300) {
            tracker.update(loudSignal)
        }
        val unfrozenFloor = tracker.noiseFloor(0)
        assertTrue(
            "Unfrozen tracker should have raised floor substantially towards tone energy: $unfrozenFloor > $initialFloor",
            unfrozenFloor > initialFloor * 10.0f
        )
    }

    @Test
    fun testDigitalSilenceClampsAtMinimumFloorWithoutNanOrInf() {
        val tracker = NoiseFloorTracker(4, NoiseTrackerParams(minFloorPower = 1e-7f, initialFloorPower = 1e-5f))
        val zeroEnergies = FloatArray(4) { 0.0f }

        // Feed 1000 hops of digital silence
        for (i in 0 until 1000) {
            tracker.update(zeroEnergies)
        }

        for (t in 0 until 4) {
            val f = tracker.noiseFloor(t)
            assertFalse("Noise floor must not be NaN", f.isNaN())
            assertFalse("Noise floor must not be infinite", f.isInfinite())
            assertEquals("Noise floor must clamp at minFloorPower", 1e-7f, f, 1e-9f)
            assertTrue("Threshold must be positive", tracker.threshold(t) > 0.0f)
        }

        val ambientDbfs = tracker.ambientLevelDbfs()
        assertFalse(ambientDbfs.isNaN())
        assertFalse(ambientDbfs.isInfinite())
        assertEquals(-70.0f, ambientDbfs, 0.1f)
    }

    @Test
    fun testDetectionThresholdAndSnrCalculations() {
        val marginDb = 6.0f
        val baseFloor = 1e-5f
        val tracker = NoiseFloorTracker(1, NoiseTrackerParams(marginDb = marginDb, initialFloorPower = baseFloor))

        val expectedThreshold = (baseFloor * 10.0.pow(marginDb / 10.0)).toFloat()
        assertEquals(expectedThreshold, tracker.threshold(0), 1e-7f)

        // Signal equal to floor gives 0 dB SNR
        val snrFloor = tracker.snrDb(baseFloor, 0)
        assertEquals(0.0f, snrFloor, 0.05f)

        // Signal equal to threshold gives 6 dB SNR
        val snrThreshold = tracker.snrDb(expectedThreshold, 0)
        assertEquals(6.0f, snrThreshold, 0.05f)

        // Signal 100x floor (+20 dB) gives 20 dB SNR
        val snrStrong = tracker.snrDb(baseFloor * 100.0f, 0)
        assertEquals(20.0f, snrStrong, 0.05f)
    }

    @Test
    fun testConfidenceMetricBehavior() {
        val tracker = NoiseFloorTracker(1, NoiseTrackerParams(marginDb = 6.0f, initialFloorPower = 1e-5f))
        val th = tracker.threshold(0)

        // 1. Below threshold gives 0.0f
        val confSubThreshold = tracker.confidence(winnerEnergy = th * 0.9f, runnerUpEnergy = 0.0f, toneIndex = 0)
        assertEquals(0.0f, confSubThreshold, 0.0f)

        // 2. High SNR but winning and runner-up energies are identical (collision) -> confidence near 0
        val confCollision = tracker.confidence(winnerEnergy = th * 10.0f, runnerUpEnergy = th * 9.99f, toneIndex = 0)
        assertTrue("Collision confidence ($confCollision) should be near 0", confCollision < 0.05f)

        // 3. Clear winner with high SNR and no runner up -> confidence near 1.0 (> 0.95 at 20x threshold)
        val confClearWinner = tracker.confidence(winnerEnergy = th * 20.0f, runnerUpEnergy = 0.0f, toneIndex = 0)
        assertTrue("Clear winner confidence ($confClearWinner) should be > 0.95", confClearWinner > 0.95f)
    }

    @Test
    fun testZeroAllocationsOnHotPathUpdate() {
        val tracker = NoiseFloorTracker(18, NoiseTrackerParams())
        val energies = FloatArray(18) { 1e-4f }

        // Warm up JIT thoroughly
        for (i in 0 until 10000) {
            tracker.update(energies)
        }

        val threadMxBean = try {
            java.lang.management.ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
        } catch (_: Throwable) {
            null
        }

        if (threadMxBean != null && threadMxBean.isThreadAllocatedMemorySupported) {
            val allocatedBefore = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().id)

            // Update 10,000 times (100 seconds of audio tracking)
            for (i in 0 until 10000) {
                tracker.update(energies)
            }

            val allocatedAfter = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().id)
            val totalAllocated = allocatedAfter - allocatedBefore
            assertEquals("Noise floor update must perform 0 heap allocations", 0L, totalAllocated)
        }
    }
}
