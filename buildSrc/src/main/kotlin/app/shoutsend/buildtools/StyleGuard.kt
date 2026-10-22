package app.shoutsend.buildtools

import java.io.File

data class StyleViolation(
    val file: File,
    val line: Int,
    val rule: String,
    val detail: String
) {
    override fun toString(): String = "${file.path}:$line: [$rule] $detail"
}

object StyleGuard {

    private val HEX_PATTERN = Regex("""#[0-9A-Fa-f]{6,8}|Color\(0x[0-9A-Fa-f]+""")
    private val NAMED_COLOR_PATTERN = Regex("""Color\.(Red|Green|Blue|Yellow|Cyan|Magenta|Black|White|DarkGray|Gray|LightGray)\b""")
    private val SHAPE_LITERAL_PATTERN = Regex("""(RoundedCornerShape|CornerSize|CutCornerShape)\s*\(\s*[0-9]+""")
    private val DP_SP_PATTERN = Regex("""\b[0-9]+(\.[0-9]+)?\s*\.\s*(dp|sp)\b""")
    private val XML_HEX_PATTERN = Regex("""android:(background|textColor|tint|fillColor|strokeColor)\s*=\s*"#[0-9A-Fa-f]{3,8}"""")

    fun scan(sourceDir: File, allowlistFile: File? = null): List<StyleViolation> {
        val violations = mutableListOf<StyleViolation>()
        val allowlist = loadAllowlist(allowlistFile)

        if (!sourceDir.exists()) return violations

        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "kt" || file.extension == "xml")) {
                scanFile(file, violations, allowlist)
            }
        }

        return violations
    }

    private fun scanFile(file: File, violations: MutableList<StyleViolation>, allowlist: Set<String>) {
        val normalizedPath = file.invariantSeparatorsPath
        val isDefaultTokens = file.name == "DefaultTokens.kt"
        val isInsideDesignSystem = normalizedPath.contains("core/designsystem")

        file.forEachLineIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()

            // Skip comments
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) return@forEachLineIndexed

            // Check allowlist
            if (allowlist.any { normalizedPath.endsWith(it) || "$normalizedPath:$lineNumber" == it }) {
                return@forEachLineIndexed
            }

            if (file.extension == "kt") {
                // Rule 6: No hex colors outside DefaultTokens.kt
                if (!isDefaultTokens && HEX_PATTERN.containsMatchIn(line)) {
                    val match = HEX_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 6 (No Hex)", "Raw hex literal '$match' forbidden outside DefaultTokens.kt"))
                }

                // Rule 6b: No named color constants
                if (NAMED_COLOR_PATTERN.containsMatchIn(line)) {
                    val match = NAMED_COLOR_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 6 (No Named Colors)", "Named color constant '$match' forbidden. Use ChirpTheme.colors tokens."))
                }

                // Rule 7: No shape literals outside core/designsystem
                if (!isInsideDesignSystem && SHAPE_LITERAL_PATTERN.containsMatchIn(line)) {
                    val match = SHAPE_LITERAL_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 7 (No Shape Literals)", "Literal shape instantiation '$match' forbidden. Use ChirpTheme.shapes tokens."))
                }

                // Rule 7b: No numeric dp/sp literals outside core/designsystem
                if (!isInsideDesignSystem && DP_SP_PATTERN.containsMatchIn(line)) {
                    val match = DP_SP_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 7 (No dp/sp Literals)", "Dimension literal '$match' forbidden. Use ChirpTheme.spacing / ChirpTheme.sizes tokens."))
                }
            } else if (file.extension == "xml") {
                // Rule 6 in XML: No hardcoded color hex in layouts/drawables
                if (XML_HEX_PATTERN.containsMatchIn(line)) {
                    val match = XML_HEX_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 6 (No XML Hex)", "Hardcoded color attribute '$match' forbidden in XML. Use theme color tokens."))
                }
            }
        }
    }

    private fun loadAllowlist(file: File?): Set<String> {
        if (file == null || !file.exists()) return emptySet()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
    }

    private inline fun File.forEachLineIndexed(action: (Int, String) -> Unit) {
        useLines { lines ->
            var idx = 0
            for (line in lines) {
                action(idx++, line)
            }
        }
    }
}
