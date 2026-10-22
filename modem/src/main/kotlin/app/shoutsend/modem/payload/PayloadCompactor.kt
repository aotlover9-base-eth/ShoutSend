package app.shoutsend.modem.payload

import app.shoutsend.modem.packet.MessageType

/**
 * Result of the compactor chooser containing the final on-air byte buffer and compaction flag.
 *
 * @property bytes The selected on-air bytes (guaranteed to be the shortest representation).
 * @property isCompact True if the compact format was selected over canonical format.
 */
public data class CompactResult(
    public val bytes: ByteArray,
    public val isCompact: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompactResult) return false
        if (isCompact != other.isCompact) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + isCompact.hashCode()
        return result
    }
}

/**
 * Unified compactor and expander API for ShoutSend application payloads.
 *
 * The chooser guarantees that the compact form is selected ONLY when it is strictly shorter
 * than the canonical representation, eliminating on-air expansion overhead.
 */
public object PayloadCompactor {

    /**
     * Compacts [payload], selecting whichever representation (canonical or compact) is strictly shorter.
     */
    public fun compact(payload: Payload): CompactResult {
        val canonicalBytes = PayloadCodec.encode(payload)

        val candidateCompactBytes = when (payload) {
            is Payload.Text -> {
                if (SevenBitPacker.is7BitAscii(payload.value)) {
                    SevenBitPacker.pack(payload.value)
                } else {
                    null
                }
            }
            is Payload.Url -> UrlCompactor.compact(payload.value)
            is Payload.WiFi -> WifiCompactor.compact(payload)
            is Payload.Contact -> ContactCompactor.compact(payload)
        }

        return if (candidateCompactBytes != null && candidateCompactBytes.size < canonicalBytes.size) {
            CompactResult(bytes = candidateCompactBytes, isCompact = true)
        } else {
            CompactResult(bytes = canonicalBytes, isCompact = false)
        }
    }

    /**
     * Expands [bytes] according to [isCompact] flag and [type].
     *
     * Never throws exceptions on malformed or corrupted inputs.
     */
    public fun expand(bytes: ByteArray, isCompact: Boolean, type: MessageType): PayloadDecodeResult {
        if (!isCompact) {
            return PayloadCodec.decode(bytes, type)
        }

        return try {
            when (type) {
                MessageType.TEXT -> {
                    val text = SevenBitPacker.unpack(bytes)
                    if (text != null) {
                        val p = Payload.Text(text)
                        when (val v = p.validate()) {
                            is PayloadValidationResult.Valid -> PayloadDecodeResult.Success(p)
                            is PayloadValidationResult.Invalid -> PayloadDecodeResult.Failure(v.error.reason)
                        }
                    } else {
                        PayloadDecodeResult.Failure("Malformed 7-bit packed text")
                    }
                }
                MessageType.URL -> {
                    val url = UrlCompactor.expand(bytes)
                    if (url != null) {
                        val p = Payload.Url(url)
                        when (val v = p.validate()) {
                            is PayloadValidationResult.Valid -> PayloadDecodeResult.Success(p)
                            is PayloadValidationResult.Invalid -> PayloadDecodeResult.Failure(v.error.reason)
                        }
                    } else {
                        PayloadDecodeResult.Failure("Malformed compacted URL")
                    }
                }
                MessageType.WIFI -> {
                    val wifi = WifiCompactor.expand(bytes)
                    if (wifi != null) {
                        PayloadDecodeResult.Success(wifi)
                    } else {
                        PayloadDecodeResult.Failure("Malformed compacted Wi-Fi payload")
                    }
                }
                MessageType.CONTACT -> {
                    val contact = ContactCompactor.expand(bytes)
                    if (contact != null) {
                        PayloadDecodeResult.Success(contact)
                    } else {
                        PayloadDecodeResult.Failure("Malformed compacted Contact payload")
                    }
                }
            }
        } catch (_: Exception) {
            PayloadDecodeResult.Failure("Unexpected expansion error")
        }
    }
}
