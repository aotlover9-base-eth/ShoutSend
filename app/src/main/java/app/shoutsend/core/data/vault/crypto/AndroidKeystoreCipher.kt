package app.shoutsend.core.data.vault.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Production implementation of [KeystoreCipher] backed by a hardware-backed
 * (or TEE/SE) AES-256 key inside the AndroidKeyStore.
 */
class AndroidKeystoreCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : KeystoreCipher {

    override fun encrypt(plaintext: ByteArray): CipherResult {
        return try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)
            CipherResult.Success(ciphertext = ciphertext, iv = iv)
        } catch (e: Throwable) {
            CipherResult.Failure(e)
        }
    }

    override fun decrypt(ciphertext: ByteArray, iv: ByteArray): PlaintextResult {
        return try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plaintext = cipher.doFinal(ciphertext)
            PlaintextResult.Success(plaintext = plaintext)
        } catch (e: Throwable) {
            PlaintextResult.Failure(e)
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    companion object {
        const val DEFAULT_KEY_ALIAS: String = "shoutsend_vault_v1"
        private const val ANDROID_KEYSTORE: String = "AndroidKeyStore"
        private const val TRANSFORMATION: String = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS: Int = 128
        private const val KEY_SIZE_BITS: Int = 256
    }
}
