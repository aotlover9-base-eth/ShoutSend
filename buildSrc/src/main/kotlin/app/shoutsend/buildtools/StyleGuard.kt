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

    // Anti-slop patterns (Rules 32a & 32b)
    private val FORBIDDEN_GLYPHS = setOf('▲', '▼', '■', '●', '◆', 'ℹ', '✓', '✔', '✕', '✖')
    private val STRING_LITERAL_REGEX = Regex("""\"([^\"]*)\"""")
    private val XML_TEXT_REGEX = Regex(""">([^<]+)<""")
    private val MONOSPACE_PATTERN = Regex("""FontFamily\.Monospace""")
    private val UPPERCASE_IN_TEXT_PATTERN = Regex("""(text\s*=\s*[^,\n]*\.uppercase\(\)|Text\s*\([^)\n]*\.uppercase\(\))""")

    private fun isEmoji(cp: Int): Boolean {
        return (cp in 0x1F300..0x1F9FF) ||
               (cp in 0x1FA00..0x1FAFF) ||
               (cp in 0x2600..0x27BF) ||
               (cp in 0xFE00..0xFE0F) ||
               (cp in 0x1F1E6..0x1F1FF) ||
               (cp in 0x2300..0x23FF) ||
               (cp in 0x2B00..0x2BFF) ||
               cp == 0x203C || cp == 0x2049 || cp == 0x2139
    }

    private fun findForbiddenGlyphOrEmoji(text: String): String? {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (text[i] in FORBIDDEN_GLYPHS) {
                return text[i].toString()
            }
            if (isEmoji(cp)) {
                return String(Character.toChars(cp))
            }
            i += Character.charCount(cp)
        }
        return null
    }

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
        val isMainSource = normalizedPath.contains("src/main/")
        val isDspLab = normalizedPath.contains("DspLab")

        file.forEachLineIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()

            // Skip comments
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) return@forEachLineIndexed

            // Check allowlist
            val lineTarget = "$normalizedPath:$lineNumber"
            if (allowlist.any { entry ->
                normalizedPath.endsWith(entry) ||
                lineTarget.endsWith(entry) ||
                lineTarget == entry ||
                (entry.contains(":") && lineTarget.endsWith(entry.substringBeforeLast(":")) && lineNumber == entry.substringAfterLast(":").toIntOrNull())
            }) {
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

                // Anti-slop checks for app/src/main
                if (isMainSource) {
                    // Rule 32b: No FontFamily.Monospace in user-facing UI (dev DSP Lab is exempt)
                    if (!isDspLab && MONOSPACE_PATTERN.containsMatchIn(line)) {
                        violations.add(StyleViolation(file, lineNumber, "Rule 32b (No Monospace)", "FontFamily.Monospace forbidden in user-facing UI code."))
                    }

                    // Rule 32b: No .uppercase() applied to text passed to Text()
                    if (UPPERCASE_IN_TEXT_PATTERN.containsMatchIn(line)) {
                        violations.add(StyleViolation(file, lineNumber, "Rule 32b (No Uppercase)", ".uppercase() applied to UI text passed to Text() is forbidden. Use sentence case."))
                    }

                    // Rule 32a: No emoji or glyph icons in string literals
                    for (m in STRING_LITERAL_REGEX.findAll(line)) {
                        val str = m.groupValues[1]
                        val forbidden = findForbiddenGlyphOrEmoji(str)
                        if (forbidden != null) {
                            val isGlyph = forbidden.length == 1 && forbidden[0] in FORBIDDEN_GLYPHS
                            val ruleType = if (isGlyph) "Rule 32a (No Glyph Icons)" else "Rule 32a (No Emoji Icons)"
                            val detail = if (isGlyph) "Forbidden glyph '$forbidden' found in UI string literal. Use Material Symbols drawables."
                                         else "Emoji icon '$forbidden' found in UI string literal. Use Material Symbols drawables."
                            violations.add(StyleViolation(file, lineNumber, ruleType, detail))
                        }
                    }
                }
            } else if (file.extension == "xml") {
                // Rule 6 in XML: No hardcoded color hex in layouts/drawables
                if (XML_HEX_PATTERN.containsMatchIn(line)) {
                    val match = XML_HEX_PATTERN.find(line)?.value ?: ""
                    violations.add(StyleViolation(file, lineNumber, "Rule 6 (No XML Hex)", "Hardcoded color attribute '$match' forbidden in XML. Use theme color tokens."))
                }

                if (isMainSource) {
                    val textMatch = XML_TEXT_REGEX.find(line)
                    val str = textMatch?.groupValues?.get(1) ?: STRING_LITERAL_REGEX.find(line)?.groupValues?.get(1)
                    if (str != null) {
                        val forbidden = findForbiddenGlyphOrEmoji(str)
                        if (forbidden != null) {
                            val isGlyph = forbidden.length == 1 && forbidden[0] in FORBIDDEN_GLYPHS
                            val ruleType = if (isGlyph) "Rule 32a (No Glyph Icons)" else "Rule 32a (No Emoji Icons)"
                            val detail = if (isGlyph) "Forbidden glyph '$forbidden' found in XML string. Use Material Symbols drawables."
                                         else "Emoji icon '$forbidden' found in XML string. Use Material Symbols drawables."
                            violations.add(StyleViolation(file, lineNumber, ruleType, detail))
                        }
                    }
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
