package app.shoutsend.core.data.vault.model

import app.shoutsend.modem.payload.Payload

/**
 * Decoded vault item exposed to the UI layer.
 *
 * Sensitive content is strictly masked in [toString] per Rule 25:
 * decrypted payload body, credentials, SSIDs, and passwords must NEVER
 * appear in logs or string representations.
 */
data class VaultItem(
    val id: Long,
    val timestamp: Long,
    val direction: Direction,
    val payload: Payload?,
    val isUnreadable: Boolean,
    val isSecure: Boolean,
    val isStarred: Boolean,
    val profileSummary: String,
    val durationMs: Long,
    val snrDb: Float?
) {
    override fun toString(): String {
        val payloadType = when {
            isUnreadable -> "UNREADABLE"
            payload != null -> payload.messageType.name
            else -> "NONE"
        }
        return "VaultItem(id=$id, timestamp=$timestamp, direction=$direction, type=$payloadType, " +
            "isUnreadable=$isUnreadable, isSecure=$isSecure, isStarred=$isStarred, " +
            "profileSummary=$profileSummary, durationMs=$durationMs, snrDb=$snrDb)"
    }
}
