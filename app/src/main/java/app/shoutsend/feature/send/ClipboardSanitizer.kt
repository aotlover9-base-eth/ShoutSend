package app.shoutsend.feature.send

/**
 * Pure function utility for sanitizing clipboard text inputs prior to payload encoding.
 */
object ClipboardSanitizer {

    /**
     * Sanitizes pasted text for multiline plaintext messages.
     * Normalizes line breaks (\r\n -> \n) and strips non-printable ASCII control characters
     * (codes 0..31 except newline and tab, and 127 DEL).
     */
    fun sanitizeText(input: String): String {
        val normalized = input.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.filter { c ->
            (c.code >= 32 && c.code != 127) || c == '\n' || c == '\t'
        }
    }

    /**
     * Sanitizes pasted text for web URLs.
     * Trims whitespace and strips all control characters, interior whitespace, and newlines.
     */
    fun sanitizeUrl(input: String): String {
        return input.trim().filter { c ->
            c.code > 32 && c.code != 127
        }
    }
}
