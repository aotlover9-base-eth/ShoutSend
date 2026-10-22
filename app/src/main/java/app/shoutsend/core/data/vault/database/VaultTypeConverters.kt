package app.shoutsend.core.data.vault.database

import androidx.room.TypeConverter
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.modem.packet.MessageType

/**
 * Room type converters for storing [Direction] and [MessageType] enums as string columns.
 */
class VaultTypeConverters {

    @TypeConverter
    fun fromDirection(direction: Direction): String = direction.name

    @TypeConverter
    fun toDirection(value: String): Direction = try {
        Direction.valueOf(value)
    } catch (_: Exception) {
        Direction.RECEIVED
    }

    @TypeConverter
    fun fromMessageType(messageType: MessageType): String = messageType.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = try {
        MessageType.valueOf(value)
    } catch (_: Exception) {
        MessageType.TEXT
    }
}
