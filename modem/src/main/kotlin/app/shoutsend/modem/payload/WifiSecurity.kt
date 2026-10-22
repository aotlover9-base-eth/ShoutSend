package app.shoutsend.modem.payload

/**
 * Wi-Fi authentication and security modes supported by ShoutSend.
 */
public enum class WifiSecurity {
    OPEN,
    WPA2,
    WPA3;

    public companion object {
        public fun fromString(value: String): WifiSecurity? = when (value.uppercase()) {
            "NOPASS", "OPEN" -> OPEN
            "WPA", "WPA2" -> WPA2
            "WPA3" -> WPA3
            else -> null
        }
    }
}
