package app.shoutsend.core.data.vault.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.shoutsend.core.data.vault.entity.VaultEntity
import app.shoutsend.core.data.vault.model.Direction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local encrypted vault entries.
 */
@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_entries ORDER BY timestamp DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE direction = :direction ORDER BY timestamp DESC, id DESC")
    fun observeByDirection(direction: Direction): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultEntity): Long

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM vault_entries")
    suspend fun count(): Int

    @Query("UPDATE vault_entries SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query(
        "DELETE FROM vault_entries WHERE isStarred = 0 AND id NOT IN (" +
            "SELECT id FROM vault_entries WHERE isStarred = 0 ORDER BY timestamp DESC, id DESC LIMIT :keepCount" +
            ")"
    )
    suspend fun prune(keepCount: Int)
}
