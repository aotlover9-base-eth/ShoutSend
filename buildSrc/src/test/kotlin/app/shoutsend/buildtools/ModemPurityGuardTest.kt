package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModemPurityGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val guard = ModemPurityGuard()

    @Test
    fun passesPureKotlinFile() {
        val root = tempFolder.newFolder("src")
        val file = File(root, "AudioSpec.kt")
        file.writeText(
            "package app.shoutsend.modem\n\n" +
            "public data class AudioSpec(public val sampleRateHz: Int)\n"
        )

        val violations = guard.scan(root)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun catchesAndroidImports() {
        val root = tempFolder.newFolder("src")
        val file1 = File(root, "BadAndroid.kt")
        file1.writeText(
            "package app.shoutsend.modem\n" +
            "import android.media.AudioTrack\n\n" +
            "class BadAndroid\n"
        )

        val file2 = File(root, "BadAndroidX.kt")
        file2.writeText(
            "package app.shoutsend.modem\n" +
            "import androidx.annotation.NonNull\n\n" +
            "class BadAndroidX\n"
        )

        val violations = guard.scan(root)
        assertEquals(2, violations.size)
    }
}
