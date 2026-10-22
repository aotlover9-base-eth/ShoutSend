package app.shoutsend.core.data.vault.repository

import app.shoutsend.core.data.vault.crypto.CipherResult
import app.shoutsend.core.data.vault.crypto.KeystoreCipher
import app.shoutsend.core.data.vault.crypto.PlaintextResult
import app.shoutsend.core.data.vault.dao.VaultDao
import app.shoutsend.core.data.vault.entity.VaultEntity
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.payload.PayloadDecodeResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Production implementation of [VaultRepository].
 *
 * Messages are encrypted using [cipher] before persisting to [dao].
 * Decryption failures degrade gracefully into unreadable rows with [VaultItem.isUnreadable] = true,
 * ensuring malformed entries or lost keys never crash the history list.
 */
class DefaultVaultRepository(
    private val dao: VaultDao,
    private val cipher: KeystoreCipher,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : VaultRepository {

    override fun observeItems(): Flow<List<VaultItem>> {
        return dao.observeAllNewestFirst()
            .map { entities ->
                entities.map { decryptEntity(it) }
            }
            .flowOn(dispatcher)
    }

    override suspend fun save(
        direction: Direction,
        payload: Payload,
        profileSummary: String,
        durationMs: Long,
        snrDb: Float?,
        isSecure: Boolean
    ): Long = withContext(dispatcher) {
        val canonicalBytes = PayloadCodec.encode(payload)
        val payloadBytes = ByteArray(2 + canonicalBytes.size)
        payloadBytes[0] = VAULT_PAYLOAD_VERSION
        payloadBytes[1] = (payload.messageType.ordinal and 0xFF).toByte()
        System.arraycopy(canonicalBytes, 0, payloadBytes, 2, canonicalBytes.size)

        val cipherResult = cipher.encrypt(payloadBytes)
        val (ciphertext, iv) = when (cipherResult) {
            is CipherResult.Success -> cipherResult.ciphertext to cipherResult.iv
            is CipherResult.Failure -> throw IllegalStateException(
                "Failed to encrypt vault payload: ${cipherResult.error.message}",
                cipherResult.error
            )
        }

        val entity = VaultEntity(
            id = 0,
            timestamp = clock(),
            direction = direction,
            messageType = payload.messageType,
            encryptedPayload = ciphertext,
            iv = iv,
            profileSummary = profileSummary,
            durationMs = durationMs,
            snrDb = snrDb,
            isSecure = isSecure,
            isStarred = false
        )

        dao.insert(entity)
    }

    override suspend fun delete(id: Long): Unit = withContext(dispatcher) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll(): Unit = withContext(dispatcher) {
        dao.deleteAll()
    }

    override suspend fun setStarred(id: Long, isStarred: Boolean): Unit = withContext(dispatcher) {
        dao.setStarred(id, isStarred)
    }

    override suspend fun prune(maxEntries: Int): Unit = withContext(dispatcher) {
        val keep = maxEntries.coerceAtLeast(0)
        dao.prune(keep)
    }

    private fun decryptEntity(entity: VaultEntity): VaultItem {
        val plaintextResult = try {
            cipher.decrypt(entity.encryptedPayload, entity.iv)
        } catch (t: Throwable) {
            PlaintextResult.Failure(t)
        }

        if (plaintextResult !is PlaintextResult.Success) {
            return createUnreadableItem(entity)
        }

        val plaintext = plaintextResult.plaintext
        if (plaintext.size < 2 || plaintext[0] != VAULT_PAYLOAD_VERSION) {
            return createUnreadableItem(entity)
        }

        val typeOrdinal = plaintext[1].toInt() and 0xFF
        val messageType = MessageType.entries.firstOrNull { it.ordinal == typeOrdinal } ?: entity.messageType
        val canonicalBytes = plaintext.copyOfRange(2, plaintext.size)

        val decodedPayload = when (val decodeResult = PayloadCodec.decode(canonicalBytes, messageType)) {
            is PayloadDecodeResult.Success -> decodeResult.payload
            is PayloadDecodeResult.Failure -> null
        }

        return VaultItem(
            id = entity.id,
            timestamp = entity.timestamp,
            direction = entity.direction,
            payload = decodedPayload,
            isUnreadable = decodedPayload == null,
            isSecure = entity.isSecure,
            isStarred = entity.isStarred,
            profileSummary = entity.profileSummary,
            durationMs = entity.durationMs,
            snrDb = entity.snrDb
        )
    }

    private fun createUnreadableItem(entity: VaultEntity): VaultItem {
        return VaultItem(
            id = entity.id,
            timestamp = entity.timestamp,
            direction = entity.direction,
            payload = null,
            isUnreadable = true,
            isSecure = entity.isSecure,
            isStarred = entity.isStarred,
            profileSummary = entity.profileSummary,
            durationMs = entity.durationMs,
            snrDb = entity.snrDb
        )
    }

    companion object {
        const val VAULT_PAYLOAD_VERSION: Byte = 0x01
    }
}
