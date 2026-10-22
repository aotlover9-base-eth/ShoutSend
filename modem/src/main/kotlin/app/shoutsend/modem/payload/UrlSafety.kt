package app.shoutsend.modem.payload

import java.net.URI

/**
 * Heuristic security analysis of a URL for phishing and deception detection.
 *
 * @property displayHost The normalized lowercase hostname for display to the user.
 * @property hasUserInfo True if URL contains user credentials (e.g. `user:pass@host` or the `google.com@evil.com` deception trick).
 * @property hostIsIpLiteral True if the host is a raw IPv4 or IPv6 address literal.
 * @property hostHasNonAscii True if the host contains Punycode (`xn--`) or internationalized Unicode characters.
 * @property nonStandardPort True if an explicit non-default TCP port is specified (not 80 for HTTP, not 443 for HTTPS).
 * @property veryLong True if the URL length exceeds 200 characters.
 */
public data class UrlSafetyReport(
    public val displayHost: String,
    public val hasUserInfo: Boolean,
    public val hostIsIpLiteral: Boolean,
    public val hostHasNonAscii: Boolean,
    public val nonStandardPort: Boolean,
    public val veryLong: Boolean
) {
    /**
     * True if any suspicious security indicator was detected.
     */
    public val isSuspicious: Boolean
        get() = hasUserInfo || hostIsIpLiteral || hostHasNonAscii || nonStandardPort || veryLong
}

/**
 * Analyzes URLs for deceptive patterns and visual spoofing.
 */
public object UrlSafety {

    private val IPV4_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    /**
     * Analyzes [url] and produces an immutable [UrlSafetyReport].
     *
     * Never throws exceptions on malformed URL inputs.
     */
    public fun analyze(url: String): UrlSafetyReport {
        val trimmed = url.trim()
        val isVeryLong = trimmed.length > 200

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            null
        }

        val rawUserInfo = uri?.rawUserInfo
        val hasUserInfo = !rawUserInfo.isNullOrEmpty() || hasDeceptiveAtSign(trimmed)

        val rawHost = uri?.host ?: extractFallbackHost(trimmed)
        val lowerHost = rawHost.lowercase()

        val isIpLiteral = checkIpLiteral(lowerHost)
        val hasNonAscii = checkNonAsciiOrPunycode(lowerHost)

        val port = uri?.port ?: -1
        val scheme = uri?.scheme?.lowercase()
        val isNonStandardPort = when {
            port == -1 -> false
            scheme == "http" && port == 80 -> false
            scheme == "https" && port == 443 -> false
            else -> true
        }

        return UrlSafetyReport(
            displayHost = lowerHost,
            hasUserInfo = hasUserInfo,
            hostIsIpLiteral = isIpLiteral,
            hostHasNonAscii = hasNonAscii,
            nonStandardPort = isNonStandardPort,
            veryLong = isVeryLong
        )
    }

    private fun hasDeceptiveAtSign(url: String): Boolean {
        val withoutScheme = url.substringAfter("://", "")
        if (withoutScheme.isEmpty()) return false
        val authorityPart = withoutScheme.substringBefore('/').substringBefore('?')
        return authorityPart.contains('@')
    }

    private fun extractFallbackHost(url: String): String {
        val withoutScheme = url.substringAfter("://", url)
        val authority = withoutScheme.substringBefore('/').substringBefore('?')
        val hostPart = authority.substringAfterLast('@')
        return hostPart.substringBefore(':')
    }

    private fun checkIpLiteral(host: String): Boolean {
        if (host.startsWith("[") && host.endsWith("]")) return true
        if (host.count { it == ':' } >= 2) return true // Unbracketed IPv6
        if (IPV4_REGEX.matches(host)) {
            val parts = host.split('.')
            return parts.all {
                val num = it.toIntOrNull()
                num != null && num in 0..255
            }
        }
        return false
    }

    private fun checkNonAsciiOrPunycode(host: String): Boolean {
        if (host.contains("xn--")) return true
        return host.any { it.code > 127 }
    }
}
