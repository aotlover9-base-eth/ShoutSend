package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkPurityGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun catchesDevMarkerInDex() {
        val apkFile = tempFolder.newFile("test-user.apk")
        val markersFile = tempFolder.newFile("dev-markers.txt")
        markersFile.writeText("DeveloperPanel\nDspLab\n")

        // Create zip with classes.dex containing DeveloperPanel
        ZipOutputStream(FileOutputStream(apkFile)).use { zos ->
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("Lapp/shoutsend/core/devtools/DeveloperPanel;".toByteArray())
            zos.closeEntry()
        }

        val violations = ApkPurityGuard.scan(apkFile, markersFile)
        assertEquals(1, violations.size)
        assertEquals("DeveloperPanel", violations[0].marker)
    }

    @Test
    fun passesCleanApk() {
        val apkFile = tempFolder.newFile("test-clean.apk")
        val markersFile = tempFolder.newFile("dev-markers.txt")
        markersFile.writeText("DeveloperPanel\nDspLab\n")

        ZipOutputStream(FileOutputStream(apkFile)).use { zos ->
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("Lapp/shoutsend/MainActivity;".toByteArray())
            zos.closeEntry()
        }

        val violations = ApkPurityGuard.scan(apkFile, markersFile)
        assertEquals(0, violations.size)
    }
}
