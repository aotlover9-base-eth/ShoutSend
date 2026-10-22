package app.shoutsend.modem.payload

/**
 * Prefix and suffix dictionary compactor for web URLs.
 */
public object UrlCompactor {

    public val PREFIX_TABLE: List<String> = listOf(
        "https://www.",                 // 0
        "https://",                     // 1
        "http://www.",                  // 2
        "http://",                      // 3
        "https://maps.google.com/?q=",  // 4
        "https://maps.app.goo.gl/",     // 5
        "https://github.com/",          // 6
        "https://t.me/",                // 7
        "https://wa.me/",               // 8
        "https://x.com/",               // 9
        "https://instagram.com/",       // 10
        "https://youtube.com/watch?v=", // 11
        "https://youtu.be/",            // 12
        "https://linkedin.com/in/",     // 13
        "https://spotify.link/",        // 14
        ""                              // 15 (No prefix)
    )

    public val SUFFIX_TABLE: List<String> = listOf(
        "",       // 0 (No suffix)
        "/",      // 1
        ".com",   // 2
        ".com/",  // 3
        ".org",   // 4
        ".org/",  // 5
        ".net",   // 6
        ".net/",  // 7
        ".io",    // 8
        ".io/",   // 9
        ".in",    // 10
        ".in/",   // 11
        ".co",    // 12
        ".co/",   // 13
        ".html",  // 14
        ".php"    // 15
    )

    /**
     * Compacts [url] into a 2-byte dictionary indexed byte array followed by middle UTF-8 bytes.
     */
    public fun compact(url: String): ByteArray {
        val trimmed = url.trim()

        // Find longest matching prefix
        var bestPrefixIdx = 15
        var bestPrefixLen = 0
        for (i in 0 until PREFIX_TABLE.size) {
            val prefix = PREFIX_TABLE[i]
            if (prefix.isNotEmpty() && trimmed.startsWith(prefix, ignoreCase = true)) {
                if (prefix.length > bestPrefixLen) {
                    bestPrefixLen = prefix.length
                    bestPrefixIdx = i
                }
            }
        }

        val afterPrefix = trimmed.substring(bestPrefixLen)

        // Find longest matching suffix
        var bestSuffixIdx = 0
        var bestSuffixLen = 0
        for (i in 0 until SUFFIX_TABLE.size) {
            val suffix = SUFFIX_TABLE[i]
            if (suffix.isNotEmpty() && afterPrefix.endsWith(suffix, ignoreCase = true)) {
                if (suffix.length > bestSuffixLen) {
                    bestSuffixLen = suffix.length
                    bestSuffixIdx = i
                }
            }
        }

        val middle = afterPrefix.substring(0, afterPrefix.length - bestSuffixLen)
        val middleBytes = middle.toByteArray(Charsets.UTF_8)

        val out = ByteArray(2 + middleBytes.size)
        out[0] = (bestPrefixIdx and 0xFF).toByte()
        out[1] = (bestSuffixIdx and 0xFF).toByte()
        System.arraycopy(middleBytes, 0, out, 2, middleBytes.size)
        return out
    }

    /**
     * Expands compacted bytes into original URL string.
     * Returns null if bytes are malformed.
     */
    public fun expand(bytes: ByteArray): String? {
        if (bytes.size < 2) return null
        val prefixIdx = bytes[0].toInt() and 0xFF
        val suffixIdx = bytes[1].toInt() and 0xFF
        if (prefixIdx >= PREFIX_TABLE.size || suffixIdx >= SUFFIX_TABLE.size) return null

        val middle = String(bytes, 2, bytes.size - 2, Charsets.UTF_8)
        return PREFIX_TABLE[prefixIdx] + middle + SUFFIX_TABLE[suffixIdx]
    }
}
