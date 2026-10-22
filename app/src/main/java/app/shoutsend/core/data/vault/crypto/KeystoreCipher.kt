package app.shoutsend.core.data.vault.crypto

/**
 * Contract for encrypting and decrypting vault payloads at rest using AES-GCM.
 */
interface KeystoreCipher {
    fun encrypt(plaintext: ByteArray): CipherResult
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): PlaintextResult
}

sealed class CipherResult {
    data class Success(val ciphertext: ByteArray, val iv: ByteArray) : CipherResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
        }

        override fun hashCode(): Int {
            return 31 * ciphertext.contentHashCode() + iv.contentHashCode()
        }
    }

    data class Failure(val error: Throwable) : CipherResult()
}

sealed class PlaintextResult {
    data class Success(val plaintext: ByteArray) : PlaintextResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return plaintext.contentEquals(other.plaintext)
        }

        override fun hashCode(): Int {
            return plaintext.contentHashCode()
        }
    }

    data class Failure(val error: Throwable) : PlaintextResult()
}
