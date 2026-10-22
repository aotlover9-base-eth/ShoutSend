package app.shoutsend.modem.fec

/**
 * Galois Field GF(2^8) arithmetic with primitive polynomial 0x11D (x^8 + x^4 + x^3 + x^2 + 1)
 * and primitive generator alpha = 2.
 *
 * Uses doubled exponent lookup table (size 512) to eliminate modulo arithmetic in multiplication hot paths.
 */
public object Gf256 {

    public const val PRIMITIVE_POLYNOMIAL: Int = 0x11D
    public const val FIELD_SIZE: Int = 256
    public const val FIELD_ORDER: Int = 255

    public val exp: IntArray = IntArray(512)
    public val log: IntArray = IntArray(256)

    init {
        var x = 1
        for (i in 0 until FIELD_ORDER) {
            exp[i] = x
            exp[i + FIELD_ORDER] = x
            log[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor PRIMITIVE_POLYNOMIAL
            }
        }
        log[0] = 0 // By convention; 0 has no logarithm in GF(2^8)
    }

    public fun add(a: Int, b: Int): Int = a xor b

    public fun sub(a: Int, b: Int): Int = a xor b

    public fun multiply(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return exp[log[a and 0xFF] + log[b and 0xFF]]
    }

    public fun divide(a: Int, b: Int): Int {
        if (b == 0) throw ArithmeticException("Division by zero in GF(256)")
        if (a == 0) return 0
        val diff = log[a and 0xFF] - log[b and 0xFF] + FIELD_ORDER
        return exp[diff % FIELD_ORDER]
    }

    public fun inverse(a: Int): Int {
        if (a == 0) throw ArithmeticException("Zero has no multiplicative inverse in GF(256)")
        return exp[FIELD_ORDER - log[a and 0xFF]]
    }

    public fun power(a: Int, n: Int): Int {
        if (a == 0) return if (n == 0) 1 else 0
        if (n == 0) return 1
        val p = ((log[a and 0xFF] * n) % FIELD_ORDER + FIELD_ORDER) % FIELD_ORDER
        return exp[p]
    }

    /**
     * Multiplies two polynomials [p] and [q] in GF(256).
     * Polynomial coefficients are ordered from highest degree to constant term (degree 0).
     */
    public fun polyMultiply(p: IntArray, q: IntArray): IntArray {
        if (p.isEmpty() || q.isEmpty()) return IntArray(0)
        val result = IntArray(p.size + q.size - 1)
        for (i in p.indices) {
            val pi = p[i]
            if (pi == 0) continue
            for (j in q.indices) {
                result[i + j] = result[i + j] xor multiply(pi, q[j])
            }
        }
        return result
    }

    /**
     * Adds two polynomials [p] and [q] in GF(256) (aligned by lower degree terms).
     */
    public fun polyAdd(p: IntArray, q: IntArray): IntArray {
        val outSize = maxOf(p.size, q.size)
        val result = IntArray(outSize)
        val pOffset = outSize - p.size
        val qOffset = outSize - q.size

        for (i in p.indices) {
            result[i + pOffset] = result[i + pOffset] xor p[i]
        }
        for (i in q.indices) {
            result[i + qOffset] = result[i + qOffset] xor q[i]
        }
        return result
    }

    /**
     * Evaluates polynomial [p] at [x] in GF(256) using Horner's method.
     * Coefficients are ordered highest degree to constant term.
     */
    public fun polyEvaluate(p: IntArray, x: Int): Int {
        if (p.isEmpty()) return 0
        var result = 0
        for (coeff in p) {
            result = multiply(result, x) xor coeff
        }
        return result
    }
}
