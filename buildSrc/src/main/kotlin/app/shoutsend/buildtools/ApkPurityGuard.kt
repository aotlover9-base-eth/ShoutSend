package app.shoutsend.buildtools

import java.io.File
import java.util.zip.ZipFile

data class PurityViolation(
    val apkFile: File,
    val dexEntry: String,
    val marker: String
) {
    override fun toString(): String = "${apkFile.name} [$dexEntry]: contains forbidden dev marker '$marker'"
}

object ApkPurityGuard {

    fun scan(apkFile: File, markersFile: File): List<PurityViolation> {
        val violations = mutableListOf<PurityViolation>()
        if (!apkFile.exists()) {
            throw IllegalArgumentException("APK file not found: ${apkFile.absolutePath}")
        }
        if (!markersFile.exists()) {
            throw IllegalArgumentException("Markers file not found: ${markersFile.absolutePath}")
        }

        val markers = markersFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        ZipFile(apkFile).use { zip ->
            val dexEntries = zip.entries().asSequence().filter { it.name.endsWith(".dex") }.toList()
            for (entry in dexEntries) {
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                val contentString = String(bytes, Charsets.ISO_8859_1)
                for (marker in markers) {
                    if (contentString.contains(marker)) {
                        violations.add(PurityViolation(apkFile, entry.name, marker))
                    }
                }
            }
        }

        return violations
    }
}
