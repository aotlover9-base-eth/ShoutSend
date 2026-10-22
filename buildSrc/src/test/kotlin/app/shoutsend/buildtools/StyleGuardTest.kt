package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StyleGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun catchesHexColorOutsideDefaultTokens() {
        val testFile = tempFolder.newFile("MyScreen.kt")
        testFile.writeText("""
            package app.shoutsend.feature.send
            import androidx.compose.ui.graphics.Color
            val myColor = Color(0xFF112233)
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("No Hex"))
    }

    @Test
    fun allowsHexInsideDefaultTokens() {
        val testFile = tempFolder.newFile("DefaultTokens.kt")
        testFile.writeText("""
            package app.shoutsend.core.designsystem
            import androidx.compose.ui.graphics.Color
            val primary = Color(0xFF00F0FF)
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(0, violations.size)
    }

    @Test
    fun catchesNamedColorConstants() {
        val testFile = tempFolder.newFile("BadComponent.kt")
        testFile.writeText("""
            package app.shoutsend.feature.send
            import androidx.compose.ui.graphics.Color
            val c = Color.Red
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("No Named Colors"))
    }

    @Test
    fun allowsTransparentAndUnspecified() {
        val testFile = tempFolder.newFile("CleanComponent.kt")
        testFile.writeText("""
            package app.shoutsend.feature.send
            import androidx.compose.ui.graphics.Color
            val t = Color.Transparent
            val u = Color.Unspecified
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(0, violations.size)
    }

    @Test
    fun catchesShapeLiteralsOutsideDesignSystem() {
        val testFile = tempFolder.newFile("FeatureCard.kt")
        testFile.writeText("""
            package app.shoutsend.feature.receive
            import androidx.compose.foundation.shape.RoundedCornerShape
            val shape = RoundedCornerShape(12)
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("No Shape Literals"))
    }

    @Test
    fun catchesDpSpLiteralsOutsideDesignSystem() {
        val testFile = tempFolder.newFile("SpacingLeak.kt")
        testFile.writeText("""
            package app.shoutsend.feature.settings
            import androidx.compose.ui.unit.dp
            val padding = 16.dp
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("No dp/sp Literals"))
    }

    @Test
    fun respectsAllowlist() {
        val testFile = tempFolder.newFile("AllowedLeak.kt")
        testFile.writeText("""
            package app.shoutsend.feature.send
            import androidx.compose.ui.unit.dp
            val p = 8.dp
        """.trimIndent())

        val allowlist = tempFolder.newFile("allowlist.txt")
        allowlist.writeText("AllowedLeak.kt")

        val violations = StyleGuard.scan(tempFolder.root, allowlist)
        assertEquals(0, violations.size)
    }

    @Test
    fun catchesHardcodedHexInXml() {
        val testFile = tempFolder.newFile("layout.xml")
        testFile.writeText("""
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:background="#FF00FF" />
        """.trimIndent())

        val violations = StyleGuard.scan(tempFolder.root)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("No XML Hex"))
    }
}
