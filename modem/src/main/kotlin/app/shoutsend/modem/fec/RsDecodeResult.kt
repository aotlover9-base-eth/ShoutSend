package app.shoutsend.modem.fec

/**
 * Result of a Reed-Solomon error-and-erasure decoding attempt.
 */
public sealed interface RsDecodeResult {

    /**
     * Successfully decoded and corrected codeword.
     *
     * @property data The recovered systematic data payload bytes.
     * @property errorCount Number of unknown byte errors detected and corrected.
     * @property erasureCount Number of known byte erasures corrected.
     */
    public data class Corrected(
        public val data: ByteArray,
        public val errorCount: Int,
        public val erasureCount: Int
    ) : RsDecodeResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Corrected
            return data.contentEquals(other.data) &&
                    errorCount == other.errorCount &&
                    erasureCount == other.erasureCount
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + errorCount
            result = 31 * result + erasureCount
            return result
        }
    }

    /**
     * Decoding failed due to uncorrectable corruptions or structural errors.
     *
     * @property reason Descriptive diagnostic reason for the decoding failure.
     */
    public data class Failure(
        public val reason: String
    ) : RsDecodeResult
}
