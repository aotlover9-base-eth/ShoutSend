package app.shoutsend.modem.fec

import app.shoutsend.modem.profile.RsProfile

/**
 * Systematic Reed-Solomon encoder for GF(256).
 *
 * Codewords consist of data bytes followed by parity bytes.
 * Generator polynomial roots are alpha^0, alpha^1, ..., alpha^(nsym-1) (first consecutive root 0).
 * Supports shortened codewords when data.size < profile.k.
 */
public object RsEncoder {

    // Precomputed generator polynomials for supported profiles
    private val GENERATORS: Map<RsProfile, IntArray> = mapOf(
        RsProfile.LIGHT to buildGenerator(RsProfile.LIGHT.nsym),
        RsProfile.STANDARD to buildGenerator(RsProfile.STANDARD.nsym),
        RsProfile.STRONG to buildGenerator(RsProfile.STRONG.nsym)
    )

    /**
     * Constructs the generator polynomial for [nsym] parity symbols:
     * g(x) = (x - alpha^0)(x - alpha^1)...(x - alpha^(nsym-1))
     * Highest degree term (x^nsym) is at index 0.
     */
    public fun buildGenerator(nsym: Int): IntArray {
        require(nsym >= 1) { "nsym must be at least 1, got $nsym" }
        var g = intArrayOf(1)
        for (i in 0 until nsym) {
            val root = Gf256.exp[i]
            // multiply g(x) by (x + alpha^i)
            g = Gf256.polyMultiply(g, intArrayOf(1, root))
        }
        return g
    }

    /**
     * Returns the precomputed generator polynomial for the given [profile].
     */
    public fun getGenerator(profile: RsProfile): IntArray {
        return GENERATORS[profile] ?: buildGenerator(profile.nsym)
    }

    /**
     * Encodes [data] systematically according to [profile].
     * Returns an array containing the data followed by [profile.nsym] parity bytes.
     */
    public fun encode(data: ByteArray, profile: RsProfile = RsProfile.STANDARD): ByteArray {
        val totalLength = data.size + profile.nsym
        val destination = ByteArray(totalLength)
        encode(data, profile, destination, 0)
        return destination
    }

    /**
     * Allocation-free systematic encoding writing data + parity into [destination] at [destOffset].
     *
     * @return Number of total codeword bytes written (data.size + profile.nsym).
     */
    public fun encode(
        data: ByteArray,
        profile: RsProfile,
        destination: ByteArray,
        destOffset: Int = 0
    ): Int {
        require(profile != RsProfile.RESERVED) { "Cannot encode with RESERVED profile" }
        require(data.size <= profile.k) {
            "Data length (${data.size}) exceeds profile k (${profile.k})"
        }
        val totalLength = data.size + profile.nsym
        require(destOffset >= 0 && destOffset + totalLength <= destination.size) {
            "Destination array buffer too small for $totalLength bytes at offset $destOffset"
        }

        // Copy systematic data bytes to destination
        System.arraycopy(data, 0, destination, destOffset, data.size)

        // Compute parity bytes into destination directly
        computeParity(data, profile, destination, destOffset + data.size)

        return totalLength
    }

    /**
     * Computes the [profile.nsym] parity bytes for [data] and writes them to [parityOut].
     * Uses LFSR division algorithm without object allocations.
     */
    public fun computeParity(
        data: ByteArray,
        profile: RsProfile,
        parityOut: ByteArray,
        parityOffset: Int = 0
    ) {
        val nsym = profile.nsym
        require(parityOffset >= 0 && parityOffset + nsym <= parityOut.size) {
            "parityOut buffer too small for $nsym parity bytes at offset $parityOffset"
        }

        val g = getGenerator(profile)

        // Internal remainder buffer on stack
        val rem = IntArray(nsym)

        for (i in data.indices) {
            val b = data[i].toInt() and 0xFF
            val factor = b xor rem[0]
            for (j in 0 until nsym - 1) {
                rem[j] = rem[j + 1] xor Gf256.multiply(factor, g[j + 1])
            }
            rem[nsym - 1] = Gf256.multiply(factor, g[nsym])
        }

        // Copy remainder to parityOut
        for (j in 0 until nsym) {
            parityOut[parityOffset + j] = rem[j].toByte()
        }
    }
}
