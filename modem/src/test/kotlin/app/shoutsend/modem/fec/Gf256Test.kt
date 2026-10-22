package app.shoutsend.modem.fec

import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Gf256Test {

    private val rng = SeededRandom(404L)

    @Test
    fun multiplicativeInverseAxiom() {
        for (a in 1..255) {
            val inv = Gf256.inverse(a)
            val product = Gf256.multiply(a, inv)
            assertEquals("a * inverse(a) != 1 for a = $a", 1, product)
        }

        assertThrows(ArithmeticException::class.java) {
            Gf256.inverse(0)
        }
    }

    @Test
    fun divisionAxiom() {
        for (i in 0 until 1000) {
            val a = rng.nextInt(0, 256)
            val b = rng.nextInt(1, 256) // non-zero
            val quotient = Gf256.divide(a, b)
            val product = Gf256.multiply(quotient, b)
            assertEquals("divide/multiply consistency failed for a=$a, b=$b", a, product)
        }

        assertThrows(ArithmeticException::class.java) {
            Gf256.divide(10, 0)
        }
    }

    @Test
    fun distributivityAxiom() {
        for (i in 0 until 1000) {
            val a = rng.nextInt(0, 256)
            val b = rng.nextInt(0, 256)
            val c = rng.nextInt(0, 256)

            val left = Gf256.multiply(a, Gf256.add(b, c))
            val right = Gf256.add(Gf256.multiply(a, b), Gf256.multiply(a, c))
            assertEquals("Distributivity failed for a=$a, b=$b, c=$c", left, right)
        }
    }

    @Test
    fun expLogConsistencyAcrossEntireField() {
        for (a in 1..255) {
            val logVal = Gf256.log[a]
            val expVal = Gf256.exp[logVal]
            assertEquals("exp(log(a)) != a for a=$a", a, expVal)
        }

        for (i in 0 until 255) {
            val expVal = Gf256.exp[i]
            val logVal = Gf256.log[expVal]
            assertEquals("log(exp(i)) != i for i=$i", i, logVal)

            // Verify doubled table avoids modulo
            assertEquals("Doubled exp table mismatch at $i", expVal, Gf256.exp[i + 255])
        }
    }

    @Test
    fun polynomialEvaluation() {
        // p(x) = x + 3 -> at x=2, in GF(256), 2 + 3 = 1
        val p = intArrayOf(1, 3)
        assertEquals(1, Gf256.polyEvaluate(p, 2))
        assertEquals(0, Gf256.polyEvaluate(p, 3)) // root
    }
}
