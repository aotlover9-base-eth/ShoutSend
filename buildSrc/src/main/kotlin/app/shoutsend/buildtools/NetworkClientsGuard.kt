package app.shoutsend.buildtools

import java.io.File

data class NetworkClientViolation(
    val file: File,
    val line: Int,
    val rule: String,
    val detail: String
) {
    override fun toString(): String = "${file.path}:$line: [$rule] $detail"
}

object NetworkClientsGuard {

    private val FORBIDDEN_CLIENT_PATTERNS = listOf(
        Regex("""\bHttpURLConnection\b""") to "HttpURLConnection is forbidden by Rule 29.",
        Regex("""\bHttpsURLConnection\b""") to "HttpsURLConnection is forbidden by Rule 29.",
        Regex("""\.openConnection\s*\(""") to "openConnection(...) is forbidden by Rule 29.",
        Regex("""\b(okhttp|okhttp3)\b""", RegexOption.IGNORE_CASE) to "OkHttp client is forbidden by Rule 29.",
        Regex("""\b(ktor|io\.ktor)\b""", RegexOption.IGNORE_CASE) to "Ktor client is forbidden by Rule 29.",
        Regex("""\b(retrofit|retrofit2)\b""", RegexOption.IGNORE_CASE) to "Retrofit client is forbidden by Rule 29.",
        Regex("""\bjava\.net\.URL\s*\(""") to "java.net.URL(...) I/O is forbidden by Rule 29.",
        Regex("""\bWebView\b""") to "WebView is forbidden by Rule 29."
    )

    private val INET_ADDRESS_DYNAMIC = Regex("""InetAddress\.getByName\s*\(\s*(?!\"[^\"]*\")""")
    private val SOCKET_PATTERN = Regex("""\b(java\.net\.)?(Socket|ServerSocket)\b""")

    fun scan(sourceDirs: List<File>): List<NetworkClientViolation> {
        val violations = mutableListOf<NetworkClientViolation>()
        for (dir in sourceDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().forEach { file ->
                if (file.isFile && (file.extension == "kt" || file.extension == "java")) {
                    scanFile(file, violations)
                }
            }
        }
        return violations
    }

    private fun scanFile(file: File, violations: MutableList<NetworkClientViolation>) {
        val normalizedPath = file.invariantSeparatorsPath
        val isTransferPackage = normalizedPath.contains("core/transfer/")

        file.useLines { lines ->
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                    return@forEachIndexed
                }

                // Check forbidden clients
                for ((pattern, message) in FORBIDDEN_CLIENT_PATTERNS) {
                    if (pattern.containsMatchIn(line)) {
                        violations.add(
                            NetworkClientViolation(
                                file = file,
                                line = lineNumber,
                                rule = "Rule 29 (No Network Clients)",
                                detail = message
                            )
                        )
                    }
                }

                // Check InetAddress.getByName with non-literal argument
                if (INET_ADDRESS_DYNAMIC.containsMatchIn(line)) {
                    violations.add(
                        NetworkClientViolation(
                            file = file,
                            line = lineNumber,
                            rule = "Rule 29 (No Dynamic DNS Resolution)",
                            detail = "InetAddress.getByName(...) called with non-literal argument. Only Wi-Fi Direct static IPs permitted."
                        )
                    )
                }

                // Check Socket and ServerSocket: allowed ONLY in core/transfer/
                if (!isTransferPackage && SOCKET_PATTERN.containsMatchIn(line)) {
                    violations.add(
                        NetworkClientViolation(
                            file = file,
                            line = lineNumber,
                            rule = "Rule 29 (Socket Location Constraint)",
                            detail = "Raw Socket / ServerSocket usage is forbidden outside 'core/transfer/'."
                        )
                    )
                }
            }
        }
    }
}
