package app.shoutsend.feature.send

/**
 * Enumeration of supported application payload categories in Send UI.
 */
enum class SendPayloadType {
    TEXT,
    URL,
    WIFI,
    CONTACT
}

/**
 * Structured representation of parsed incoming shared intent content.
 *
 * @property type Selected target payload category.
 * @property content Sanitized and optionally truncated content payload.
 * @property isTruncated True if original payload exceeded the 280-byte acoustic budget.
 * @property originalByteCount The byte length of the input before truncation.
 */
data class ParsedSharedContent(
    val type: SendPayloadType,
    val content: String,
    val isTruncated: Boolean = false,
    val originalByteCount: Int = 0
)

/**
 * Pure function utility for parsing incoming ACTION_SEND intent text streams.
 */
object SharedTextParser {
    const val MAX_TEXT_BYTES: Int = 280
    const val MAX_URL_BYTES: Int = 280

    /**
     * Parses incoming shared text into a structured [ParsedSharedContent].
     *
     * Invariants:
     * - Trims input string.
     * - If empty or blank, returns TEXT type with empty content.
     * - If string starts with http:// or https:// (case-insensitive): maps to URL type.
     * - Otherwise maps to TEXT type.
     * - Content exceeding 280 UTF-8 bytes is cleanly truncated without splitting Unicode code points.
     */
    fun parse(rawText: String?): ParsedSharedContent {
        if (rawText.isNullOrBlank()) {
            return ParsedSharedContent(
                type = SendPayloadType.TEXT,
                content = "",
                isTruncated = false,
                originalByteCount = 0
            )
        }

        val trimmed = rawText.trim()
        val isUrlCandidate = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)

        val targetType = if (isUrlCandidate) SendPayloadType.URL else SendPayloadType.TEXT
        val maxBytes = if (targetType == SendPayloadType.URL) MAX_URL_BYTES else MAX_TEXT_BYTES

        val originalBytes = trimmed.toByteArray(Charsets.UTF_8).size

        return if (originalBytes <= maxBytes) {
            ParsedSharedContent(
                type = targetType,
                content = trimmed,
                isTruncated = false,
                originalByteCount = originalBytes
            )
        } else {
            val truncated = truncateToMaxUtf8Bytes(trimmed, maxBytes)
            ParsedSharedContent(
                type = targetType,
                content = truncated,
                isTruncated = true,
                originalByteCount = originalBytes
            )
        }
    }

    /**
     * Truncates [text] so that its UTF-8 representation fits within [maxBytes],
     * preserving full Unicode code points and surrogate pairs.
     */
    fun truncateToMaxUtf8Bytes(text: String, maxBytes: Int): String {
        var byteCount = 0
        val sb = StringBuilder()
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            val charCount = Character.charCount(codePoint)
            val codePointBytes = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            if (byteCount + codePointBytes > maxBytes) {
                break
            }
            byteCount += codePointBytes
            sb.appendCodePoint(codePoint)
            offset += charCount
        }
        return sb.toString()
    }
}
