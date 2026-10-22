package app.shoutsend.modem.profile

/**
 * Reed-Solomon error correction profiles for ShoutSend.
 *
 * Codeword parameters:
 * - [n]: Total codeword bytes on air.
 * - [k]: Data payload bytes.
 * - [nsym]: Parity / check bytes (n - k).
 * - [maxErrors]: Maximum correctable byte errors (nsym / 2).
 */
public enum class RsProfile(
    public val id: Int,
    public val n: Int,
    public val k: Int,
    public val nsym: Int,
    public val maxErrors: Int
) {
    LIGHT(id = 0, n = 60, k = 54, nsym = 6, maxErrors = 3),
    STANDARD(id = 1, n = 60, k = 48, nsym = 12, maxErrors = 6),
    STRONG(id = 2, n = 60, k = 40, nsym = 20, maxErrors = 10),
    RESERVED(id = 3, n = 0, k = 0, nsym = 0, maxErrors = 0);

    init {
        require(n <= 255) { "Codeword length n ($n) cannot exceed GF(256) limit of 255" }
        require(k <= n) { "Data length k ($k) cannot exceed codeword length n ($n)" }
        require(nsym == n - k) { "nsym must equal n - k" }
    }

    public val codewordBytes: Int get() = n
    public val dataBytes: Int get() = k
    public val parityBytes: Int get() = nsym

    public companion object {
        public fun fromId(id: Int): RsProfile? = entries.find { it.id == id }
    }
}
