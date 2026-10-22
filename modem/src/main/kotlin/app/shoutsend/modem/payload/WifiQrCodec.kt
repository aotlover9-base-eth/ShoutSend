package app.shoutsend.modem.payload

/**
 * Standard Wi-Fi QR code string codec with strict character escaping.
 *
 * Format:
 * `WIFI:T:<WPA|nopass>;S:<ssid>;P:<password>;H:<true|false>;;`
 *
 * Special characters (`\`, `;`, `,`, `:`, `"`) in SSID and Password are escaped with a preceding backslash.
 */
public object WifiQrCodec {

    private const val PREFIX = "WIFI:"
    private const val SUFFIX = ";;"

    /**
     * Serializes a [Payload.WiFi] into standard Wi-Fi QR string representation.
     */
    public fun encode(wifi: Payload.WiFi): String {
        val sb = StringBuilder(PREFIX)

        // T: security
        val tVal = when (wifi.security) {
            WifiSecurity.OPEN -> "nopass"
            WifiSecurity.WPA2, WifiSecurity.WPA3 -> "WPA"
        }
        sb.append("T:").append(tVal).append(';')

        // S: ssid
        sb.append("S:").append(escape(wifi.ssid)).append(';')

        // P: password (if present)
        if (!wifi.password.isNullOrEmpty() && wifi.security != WifiSecurity.OPEN) {
            sb.append("P:").append(escape(wifi.password)).append(';')
        }

        // H: hidden
        sb.append("H:").append(wifi.isHidden).append(';')

        // Final double semicolon
        sb.append(';')
        return sb.toString()
    }

    /**
     * Strictly parses a Wi-Fi QR string into a [Payload.WiFi], or returns null if malformed.
     */
    public fun decode(raw: String): Payload.WiFi? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(PREFIX, ignoreCase = true) || !trimmed.endsWith(SUFFIX)) {
            return null
        }

        val content = trimmed.substring(PREFIX.length, trimmed.length - 1) // strip PREFIX and trailing ';'
        val pairs = splitFields(content)

        var security: WifiSecurity = WifiSecurity.WPA2
        var ssid: String? = null
        var password: String? = null
        var isHidden = false

        for (pair in pairs) {
            val colonIdx = pair.indexOf(':')
            if (colonIdx <= 0) continue
            val key = pair.substring(0, colonIdx).trim().uppercase()
            val rawValue = pair.substring(colonIdx + 1)
            val unescaped = unescape(rawValue)

            when (key) {
                "T" -> {
                    security = when (unescaped.uppercase()) {
                        "NOPASS", "OPEN" -> WifiSecurity.OPEN
                        "WPA", "WPA2" -> WifiSecurity.WPA2
                        "WPA3" -> WifiSecurity.WPA3
                        else -> return null // Unknown security type
                    }
                }
                "S" -> ssid = unescaped
                "P" -> password = unescaped
                "H" -> isHidden = unescaped.equals("true", ignoreCase = true)
            }
        }

        if (ssid.isNullOrEmpty()) {
            return null
        }

        val result = Payload.WiFi(
            ssid = ssid,
            password = if (security == WifiSecurity.OPEN) null else password,
            security = security,
            isHidden = isHidden
        )

        val validation = result.validate()
        return if (validation is PayloadValidationResult.Valid) result else null
    }

    /**
     * Escapes `\`, `;`, `,`, `:`, `"` by prefixing each with `\`.
     */
    public fun escape(input: String): String {
        val sb = StringBuilder(input.length + 8)
        for (c in input) {
            if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') {
                sb.append('\\')
            }
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Unescapes backslash-escaped characters in [input].
     */
    public fun unescape(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                val next = input[i + 1]
                if (next == '\\' || next == ';' || next == ',' || next == ':' || next == '"') {
                    sb.append(next)
                    i += 2
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun splitFields(content: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\\' && i + 1 < content.length) {
                sb.append(c).append(content[i + 1])
                i += 2
                continue
            }
            if (c == ';') {
                if (sb.isNotEmpty()) {
                    fields.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) {
            fields.add(sb.toString())
        }
        return fields
    }
}
