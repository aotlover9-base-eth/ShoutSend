package app.shoutsend.core.data.vault.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.modem.packet.MessageType

/**
 * Room entity representing an encrypted local message entry stored in the local SQLite vault.
 *
 * All message contents in [encryptedPayload] are encrypted with an Android Keystore key
 * using AES-256-GCM before reaching disk. Plaintext is never stored in this table (Rule 25).
 */
@Entity(tableName = "vault_entries")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val direction: Direction,
    val messageType: MessageType,
    val encryptedPayload: ByteArray,
    val iv: ByteArray,
    val profileSummary: String,
    val durationMs: Long,
    val snrDb: Float?,
    val isSecure: Boolean,
    val isStarred: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultEntity

        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (direction != other.direction) return false
        if (messageType != other.messageType) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (profileSummary != other.profileSummary) return false
        if (durationMs != other.durationMs) return false
        if (snrDb != other.snrDb) return false
        if (isSecure != other.isSecure) return false
        if (isStarred != other.isStarred) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + profileSummary.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (snrDb?.hashCode() ?: 0)
        result = 31 * result + isSecure.hashCode()
        result = 31 * result + isStarred.hashCode()
        return result
    }

    override fun toString(): String {
        return "VaultEntity(id=$id, timestamp=$timestamp, direction=$direction, " +
            "messageType=$messageType, payloadBytes=${encryptedPayload.size}, ivBytes=${iv.size}, " +
            "profileSummary=$profileSummary, durationMs=$durationMs, snrDb=$snrDb, " +
            "isSecure=$isSecure, isStarred=$isStarred)"
    }
}
