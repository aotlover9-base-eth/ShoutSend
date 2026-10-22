package app.shoutsend.modem.payload

import app.shoutsend.modem.packet.MessageType

/**
 * Result of attempting to decode raw payload bytes into a typed [Payload].
 */
public sealed interface PayloadDecodeResult {
    public data class Success(public val payload: Payload) : PayloadDecodeResult
    public data class Failure(public val reason: String) : PayloadDecodeResult
}

/**
 * Canonical serializer and deserializer for ShoutSend application payloads.
 */
public object PayloadCodec {

    /**
     * Serializes a validated [payload] into canonical wire bytes.
     *
     * @throws IllegalArgumentException if [payload] violates validation rules.
     */
    public fun encode(payload: Payload): ByteArray {
        val validation = payload.validate()
        if (validation is PayloadValidationResult.Invalid) {
            throw IllegalArgumentException(validation.error.reason)
        }

        return when (payload) {
            is Payload.Text -> payload.value.toByteArray(Charsets.UTF_8)
            is Payload.Url -> payload.value.trim().toByteArray(Charsets.UTF_8)
            is Payload.WiFi -> WifiQrCodec.encode(payload).toByteArray(Charsets.UTF_8)
            is Payload.Contact -> ContactTlvCodec.encode(payload)
        }
    }

    /**
     * Deserializes raw wire [bytes] according to [type].
     *
     * Never throws exceptions on corrupted or random input bytes.
     */
    public fun decode(bytes: ByteArray, type: MessageType): PayloadDecodeResult {
        if (bytes.isEmpty()) {
            return PayloadDecodeResult.Failure("Payload bytes are empty")
        }

        return try {
            when (type) {
                MessageType.TEXT -> {
                    val str = String(bytes, Charsets.UTF_8)
                    val p = Payload.Text(str)
                    when (val v = p.validate()) {
                        is PayloadValidationResult.Valid -> PayloadDecodeResult.Success(p)
                        is PayloadValidationResult.Invalid -> PayloadDecodeResult.Failure(v.error.reason)
                    }
                }
                MessageType.URL -> {
                    val str = String(bytes, Charsets.UTF_8)
                    val p = Payload.Url(str)
                    when (val v = p.validate()) {
                        is PayloadValidationResult.Valid -> PayloadDecodeResult.Success(p)
                        is PayloadValidationResult.Invalid -> PayloadDecodeResult.Failure(v.error.reason)
                    }
                }
                MessageType.WIFI -> {
                    val str = String(bytes, Charsets.UTF_8)
                    val wifi = WifiQrCodec.decode(str)
                    if (wifi != null) {
                        PayloadDecodeResult.Success(wifi)
                    } else {
                        PayloadDecodeResult.Failure("Malformed Wi-Fi payload")
                    }
                }
                MessageType.CONTACT -> {
                    val contact = ContactTlvCodec.decode(bytes)
                    if (contact != null) {
                        PayloadDecodeResult.Success(contact)
                    } else {
                        PayloadDecodeResult.Failure("Malformed Contact TLV payload")
                    }
                }
            }
        } catch (_: Exception) {
            PayloadDecodeResult.Failure("Unexpected parsing failure")
        }
    }
}
