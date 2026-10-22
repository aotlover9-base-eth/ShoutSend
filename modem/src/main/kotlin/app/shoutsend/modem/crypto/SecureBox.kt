package app.shoutsend.modem.crypto

import app.shoutsend.modem.packet.MessageType
import java.security.SecureRandom
import java.util.Random
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Result of attempting to decrypt and authenticate a sealed ciphertext payload.
 */
public sealed interface SecureOpenResult {
    public data class Opened(public val plainBytes: ByteArray) : SecureOpenResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Opened) return false
            return plainBytes.contentEquals(other.plainBytes)
        }

        override fun hashCode(): Int = plainBytes.contentHashCode()
    }

    public data class WrongPassphraseOrTampered(
        public val detail: String = "Authentication tag mismatch or incorrect passphrase"
    ) : SecureOpenResult

    public data class Malformed(public val detail: String) : SecureOpenResult
}

/**
 * Authenticated Encryption with Associated Data (AEAD) container for ShoutSend transmissions.
 *
 * Cryptographic Primitive Profile:
 * - KDF: PBKDF2WithHmacSHA256 (60,000 iterations, 4-byte random salt, 256-bit AES key).
 * - AEAD: AES-256-GCM (12-byte random nonce, 96-bit authentication tag).
 * - Associated Authenticated Data (AAD): Protocol Version 1 byte + MessageType ID byte.
 * - On-Air Wire Overhead: exactly 28 bytes (`salt [4B] + nonce [12B] + tag [12B]`).
 *
 * Privacy Guarantees:
 * - Key material and intermediate buffers are zero-wiped upon completion.
 * - Plaintext and passphrases are never logged or stored.
 */
public object SecureBox {

    public const val SALT_BYTES: Int = 4
    public const val NONCE_BYTES: Int = 12
    public const val TAG_BYTES: Int = 12
    public const val OVERHEAD_BYTES: Int = SALT_BYTES + NONCE_BYTES + TAG_BYTES // 28 bytes
    public const val PBKDF2_ITERATIONS: Int = 60_000

    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"

    /**
     * Seals [plainBytes] using AES-256-GCM with a key derived from [passphrase].
     *
     * @param plainBytes The plaintext payload bytes to encrypt.
     * @param type Message payload type (bound into the authentication tag via AAD).
     * @param passphrase User passphrase (minimum 8 characters).
     * @param random Random entropy source (injectable for deterministic testing; defaults to SecureRandom).
     * @return Sealed byte array of size `plainBytes.size + 28`.
     */
    public fun seal(
        plainBytes: ByteArray,
        type: MessageType,
        passphrase: CharArray,
        random: Random = SecureRandom()
    ): ByteArray {
        require(passphrase.size >= PassphraseEvaluator.MIN_LENGTH) {
            "Passphrase must be at least ${PassphraseEvaluator.MIN_LENGTH} characters"
        }

        val salt = ByteArray(SALT_BYTES)
        val nonce = ByteArray(NONCE_BYTES)
        random.nextBytes(salt)
        random.nextBytes(nonce)

        var keyBytes: ByteArray? = null
        try {
            keyBytes = deriveKey(passphrase, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_BYTES * 8, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Bind version 1 and message type into AAD
            val aad = byteArrayOf(0x01.toByte(), type.id.toByte())
            cipher.updateAAD(aad)

            val ciphertextWithTag = cipher.doFinal(plainBytes)

            val sealed = ByteArray(SALT_BYTES + NONCE_BYTES + ciphertextWithTag.size)
            System.arraycopy(salt, 0, sealed, 0, SALT_BYTES)
            System.arraycopy(nonce, 0, sealed, SALT_BYTES, NONCE_BYTES)
            System.arraycopy(ciphertextWithTag, 0, sealed, SALT_BYTES + NONCE_BYTES, ciphertextWithTag.size)

            return sealed
        } finally {
            keyBytes?.fill(0)
        }
    }

    /**
     * Opens and authenticates [sealedBytes] using [passphrase].
     *
     * @param sealedBytes The on-air encrypted payload bytes.
     * @param type Message payload type (must match the type bound during encryption).
     * @param passphrase User passphrase.
     * @return [SecureOpenResult.Opened] on success, or a typed failure.
     */
    public fun open(
        sealedBytes: ByteArray,
        type: MessageType,
        passphrase: CharArray
    ): SecureOpenResult {
        if (sealedBytes.size < OVERHEAD_BYTES) {
            return SecureOpenResult.Malformed("Sealed payload too short (${sealedBytes.size} < $OVERHEAD_BYTES)")
        }
        if (passphrase.size < PassphraseEvaluator.MIN_LENGTH) {
            return SecureOpenResult.WrongPassphraseOrTampered("Passphrase below minimum length")
        }

        val salt = sealedBytes.copyOfRange(0, SALT_BYTES)
        val nonce = sealedBytes.copyOfRange(SALT_BYTES, SALT_BYTES + NONCE_BYTES)
        val ciphertextOffset = SALT_BYTES + NONCE_BYTES
        val ciphertextSize = sealedBytes.size - ciphertextOffset

        var keyBytes: ByteArray? = null
        try {
            keyBytes = deriveKey(passphrase, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_BYTES * 8, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val aad = byteArrayOf(0x01.toByte(), type.id.toByte())
            cipher.updateAAD(aad)

            val plainBytes = cipher.doFinal(sealedBytes, ciphertextOffset, ciphertextSize)
            return SecureOpenResult.Opened(plainBytes)
        } catch (_: AEADBadTagException) {
            return SecureOpenResult.WrongPassphraseOrTampered("Authentication tag check failed")
        } catch (e: Exception) {
            return SecureOpenResult.WrongPassphraseOrTampered("Decryption failure: ${e.message}")
        } finally {
            keyBytes?.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
