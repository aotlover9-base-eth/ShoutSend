package app.shoutsend.buildtools

import java.io.File

data class ModemPurityViolation(
    val file: File,
    val line: Int,
    val content: String,
    val message: String
)

class ModemPurityGuard {

    private val forbiddenImportPatterns = listOf(
        Regex("^\\s*import\\s+android\\..*"),
        Regex("^\\s*import\\s+androidx\\..*")
    )

    fun scan(sourceDir: File): List<ModemPurityViolation> {
        val violations = mutableListOf<ModemPurityViolation>()
        if (!sourceDir.exists()) return violations

        sourceDir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    for (pattern in forbiddenImportPatterns) {
                        if (pattern.containsMatchIn(line)) {
                            violations.add(
                                ModemPurityViolation(
                                    file = file,
                                    line = index + 1,
                                    content = line.trim(),
                                    message = "Forbidden Android import in pure JVM :modem module: '${line.trim()}'"
                                )
                            )
                        }
                    }
                }
            }
        }
        return violations
    }
}
