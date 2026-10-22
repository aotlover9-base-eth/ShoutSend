package app.shoutsend.modem.testutil

import kotlin.random.Random

/**
 * Deterministic pseudo-random generator wrapper for reproducible modem tests.
 */
class SeededRandom(seed: Long = 42L) {
    private val rng = Random(seed)

    fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        rng.nextBytes(bytes)
        return bytes
    }

    fun nextInt(from: Int = 0, until: Int = Int.MAX_VALUE): Int = rng.nextInt(from, until)

    fun nextFloat(): Float = rng.nextFloat()

    fun nextDouble(): Double = rng.nextDouble()

    fun nextBoolean(): Boolean = rng.nextBoolean()
}
