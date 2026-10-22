package app.shoutsend.core.data.vault.repository

import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.modem.payload.Payload
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing and observing encrypted vault messages.
 */
interface VaultRepository {

    /**
     * Observes all vault items sorted newest first.
     */
    fun observeItems(): Flow<List<VaultItem>>

    /**
     * Encrypts and persists a new message payload to the vault.
     *
     * @return The auto-generated database ID of the newly inserted entry.
     */
    suspend fun save(
        direction: Direction,
        payload: Payload,
        profileSummary: String,
        durationMs: Long,
        snrDb: Float?,
        isSecure: Boolean
    ): Long

    /**
     * Deletes a vault entry by its unique identifier.
     */
    suspend fun delete(id: Long)

    /**
     * Deletes all vault entries.
     */
    suspend fun deleteAll()

    /**
     * Updates the starred status of an existing entry.
     */
    suspend fun setStarred(id: Long, isStarred: Boolean)

    /**
     * Prunes the oldest unstarred vault entries keeping at most [maxEntries] unstarred items.
     * Starred items are never deleted by this operation.
     */
    suspend fun prune(maxEntries: Int = 500)
}
