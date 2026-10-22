package app.shoutsend.core.data.vault.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.shoutsend.core.data.vault.dao.VaultDao
import app.shoutsend.core.data.vault.entity.VaultEntity

/**
 * Local Room database for persistent encrypted vault message storage.
 */
@Database(
    entities = [VaultEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(VaultTypeConverters::class)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {
        const val DATABASE_NAME: String = "shoutsend_vault.db"
    }
}
