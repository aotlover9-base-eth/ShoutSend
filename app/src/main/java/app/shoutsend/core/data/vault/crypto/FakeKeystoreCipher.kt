package app.shoutsend.core.data.vault.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * In-memory test fake implementation of [KeystoreCipher] using pure JVM AES-256-GCM.
 *
 * Does not interact with AndroidKeyStore, enabling fast and deterministic unit testing.
 * Includes failure simulation flags to test error handling and unreadable row degradation.
 */
class FakeKeystoreCipher(
    var shouldFailEncryption: Boolean = false,
    var shouldFailDecryption: Boolean = false
) : KeystoreCipher {

    private val keyBytes = ByteArray(32) { (it xor 0x5A).toByte() }
    private val secretKey = SecretKeySpec(keyBytes, "AES")
    private val random = SecureRandom()

    override fun encrypt(plaintext: ByteArray): CipherResult {
        if (shouldFailEncryption) {
            return CipherResult.Failure(IllegalStateException("Simulated encryption failure"))
        }
        return try {
            val iv = ByteArray(12)
            random.nextBytes(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val ciphertext = cipher.doFinal(plaintext)
            CipherResult.Success(ciphertext = ciphertext, iv = iv)
        } catch (e: Throwable) {
            CipherResult.Failure(e)
        }
    }

    override fun decrypt(ciphertext: ByteArray, iv: ByteArray): PlaintextResult {
        if (shouldFailDecryption) {
            return PlaintextResult.Failure(IllegalStateException("Simulated decryption failure"))
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plaintext = cipher.doFinal(ciphertext)
            PlaintextResult.Success(plaintext = plaintext)
        } catch (e: Throwable) {
            PlaintextResult.Failure(e)
        }
    }

    companion object {
        private const val TRANSFORMATION: String = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS: Int = 128
    }
}
