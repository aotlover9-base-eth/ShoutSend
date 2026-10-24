package app.shoutsend.modem.demod

import app.shoutsend.modem.crypto.SecureBox
import app.shoutsend.modem.crypto.SecureOpenResult
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCompactor
import app.shoutsend.modem.payload.PayloadDecodeResult

/**
 * Turns a raw reassembled [ReceivedMessage] into a typed [Payload] using compaction expansion
 * and optional PBKDF2/AES-GCM decryption (Steps 13 to 15).
 */
public object PayloadDecoder {

    /**
     * Decodes [message] into a typed [Payload].
     *
     * @param message The reassembled message containing wire payload bytes and flags.
     * @param passphrase Optional passphrase for encrypted transmissions (Rule 25: never logged).
     * @return Decoded [Payload] or null if decryption or deserialization fails.
     */
    public fun decode(
        message: ReceivedMessage,
        passphrase: String? = null
    ): Payload? {
        val isEncrypted = message.flags.isEncrypted
        val isCompact = message.flags.isCompact

        val rawPayload = if (isEncrypted) {
            if (passphrase == null) {
                return null
            }
            val openResult = SecureBox.open(
                sealedBytes = message.payload,
                type = message.type,
                passphrase = passphrase.toCharArray()
            )
            when (openResult) {
                is SecureOpenResult.Opened -> openResult.plainBytes
                else -> return null
            }
        } else {
            message.payload
        }

        val expandResult = PayloadCompactor.expand(
            bytes = rawPayload,
            isCompact = isCompact,
            type = message.type
        )

        return when (expandResult) {
            is PayloadDecodeResult.Success -> expandResult.payload
            else -> null
        }
    }
}
