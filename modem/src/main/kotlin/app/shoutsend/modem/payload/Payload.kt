package app.shoutsend.modem.payload

import app.shoutsend.modem.packet.MessageType
import java.net.URI

/**
 * High-level typed application payload carried by ShoutSend acoustic transmissions.
 */
public sealed interface Payload {

    /** Corresponding Protocol v1 message type. */
    public val messageType: MessageType

    /** Validates payload fields against protocol invariants without leaking secret contents. */
    public fun validate(): PayloadValidationResult

    /**
     * Plaintext message payload.
     *
     * @property value The text content (up to 280 UTF-8 bytes).
     */
    public data class Text(public val value: String) : Payload {
        override val messageType: MessageType get() = MessageType.TEXT

        override fun validate(): PayloadValidationResult {
            if (value.isEmpty()) {
                return PayloadValidationResult.Invalid(PayloadValidationError("text", "Text cannot be empty"))
            }
            val byteCount = value.toByteArray(Charsets.UTF_8).size
            if (byteCount > 280) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("text", "Text exceeds maximum 280 UTF-8 bytes")
                )
            }
            return PayloadValidationResult.Valid
        }

        override fun toString(): String = "Text(length=${value.length}, bytes=${value.toByteArray(Charsets.UTF_8).size})"
    }

    /**
     * Web hyperlink payload.
     *
     * @property value URL string (http or https, trimmed, no control characters, up to 280 UTF-8 bytes).
     */
    public data class Url(public val value: String) : Payload {
        override val messageType: MessageType get() = MessageType.URL

        override fun validate(): PayloadValidationResult {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                return PayloadValidationResult.Invalid(PayloadValidationError("url", "URL cannot be empty"))
            }
            val byteCount = trimmed.toByteArray(Charsets.UTF_8).size
            if (byteCount > 280) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("url", "URL exceeds maximum 280 UTF-8 bytes")
                )
            }
            if (trimmed.any { it.code in 0..31 || it.code == 127 }) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("url", "URL contains forbidden control characters")
                )
            }

            val uri = try {
                URI(trimmed)
            } catch (_: Exception) {
                return PayloadValidationResult.Invalid(PayloadValidationError("url", "Malformed URL syntax"))
            }

            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("url", "Only http and https schemes are permitted")
                )
            }

            if (uri.host.isNullOrEmpty()) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("url", "URL must specify a non-empty host")
                )
            }

            return PayloadValidationResult.Valid
        }

        override fun toString(): String {
            val safeHost = try {
                URI(value.trim()).host ?: "unknown"
            } catch (_: Exception) {
                "invalid"
            }
            return "Url(host=$safeHost, length=${value.length})"
        }
    }

    /**
     * Wi-Fi network credential payload.
     *
     * @property ssid Network name (1..32 UTF-8 bytes).
     * @property password Network passphrase (8..63 printable ASCII or 64 hex characters; null for open networks).
     * @property security Wi-Fi security protocol (OPEN, WPA2, WPA3).
     * @property isHidden True if network SSID is hidden.
     */
    public data class WiFi(
        public val ssid: String,
        public val password: String? = null,
        public val security: WifiSecurity = WifiSecurity.WPA2,
        public val isHidden: Boolean = false
    ) : Payload {
        override val messageType: MessageType get() = MessageType.WIFI

        override fun validate(): PayloadValidationResult {
            val ssidBytes = ssid.toByteArray(Charsets.UTF_8).size
            if (ssidBytes !in 1..32) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("ssid", "SSID must be between 1 and 32 UTF-8 bytes")
                )
            }

            when (security) {
                WifiSecurity.OPEN -> {
                    if (!password.isNullOrEmpty()) {
                        return PayloadValidationResult.Invalid(
                            PayloadValidationError("password", "Open Wi-Fi cannot have a password")
                        )
                    }
                }
                WifiSecurity.WPA2, WifiSecurity.WPA3 -> {
                    if (password == null) {
                        return PayloadValidationResult.Invalid(
                            PayloadValidationError("password", "Secured Wi-Fi requires a password")
                        )
                    }
                    val isHex64 = password.length == 64 && password.all {
                        it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
                    }
                    val isAsciiPassphrase = password.length in 8..63 && password.all { it.code in 32..126 }
                    if (!isHex64 && !isAsciiPassphrase) {
                        return PayloadValidationResult.Invalid(
                            PayloadValidationError(
                                "password",
                                "WPA passphrase must be 8-63 printable ASCII characters or 64 hex digits"
                            )
                        )
                    }
                }
            }

            return PayloadValidationResult.Valid
        }

        override fun toString(): String {
            val maskedPass = if (password != null) "***" else "null"
            return "WiFi(ssid=$ssid, security=$security, hidden=$isHidden, password=$maskedPass)"
        }
    }

    /**
     * VCard-lite contact exchange payload.
     *
     * @property displayName Full name of the contact (1..60 UTF-8 bytes).
     * @property phones Phone numbers (up to 2, 1..20 chars each from digits, +, -, (, ), and space).
     * @property email Contact email address (optional, 1..80 UTF-8 bytes).
     */
    public data class Contact(
        public val displayName: String,
        public val phones: List<String> = emptyList(),
        public val email: String? = null
    ) : Payload {
        override val messageType: MessageType get() = MessageType.CONTACT

        override fun validate(): PayloadValidationResult {
            val nameBytes = displayName.toByteArray(Charsets.UTF_8).size
            if (nameBytes !in 1..60) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("displayName", "Contact displayName must be between 1 and 60 UTF-8 bytes")
                )
            }

            if (phones.size > 2) {
                return PayloadValidationResult.Invalid(
                    PayloadValidationError("phones", "Maximum 2 phone numbers allowed")
                )
            }

            for (phone in phones) {
                if (phone.isEmpty() || phone.length > 20) {
                    return PayloadValidationResult.Invalid(
                        PayloadValidationError("phones", "Phone number must be between 1 and 20 characters")
                    )
                }
                if (!phone.all { it in '0'..'9' || it in "+-() " }) {
                    return PayloadValidationResult.Invalid(
                        PayloadValidationError("phones", "Phone number contains invalid characters")
                    )
                }
            }

            if (email != null) {
                val emailBytes = email.toByteArray(Charsets.UTF_8).size
                if (emailBytes !in 1..80) {
                    return PayloadValidationResult.Invalid(
                        PayloadValidationError("email", "Email must be between 1 and 80 UTF-8 bytes")
                    )
                }
                if (!email.contains("@") || !email.contains(".")) {
                    return PayloadValidationResult.Invalid(
                        PayloadValidationError("email", "Email format must contain @ and a domain")
                    )
                }
            }

            return PayloadValidationResult.Valid
        }

        override fun toString(): String {
            return "Contact(displayName=$displayName, phonesCount=${phones.size}, hasEmail=${email != null})"
        }
    }
}
