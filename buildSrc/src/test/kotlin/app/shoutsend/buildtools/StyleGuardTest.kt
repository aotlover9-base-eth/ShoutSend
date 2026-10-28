package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StyleGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun flagsHexInNonDefaultTokens() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/SomeView.kt")
        file.writeText("""
            package test
            val c = Color(0xFF00FF00)
        """.trimIndent())

        val violations = StyleGuard.scan(srcDir)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("Rule 6"))
    }

    @Test
    fun allowsHexInDefaultTokens() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/DefaultTokens.kt")
        file.writeText("""
            package test
            val c = Color(0xFF00FF00)
        """.trimIndent())

        val violations = StyleGuard.scan(srcDir)
        assertEquals(0, violations.size)
    }

    @Test
    fun flagsEmojiAndGlyphsInMainSource() {
        val mainDir = tempFolder.newFolder("src", "main", "java")
        val file = tempFolder.newFile("src/main/java/Screen.kt")
        file.writeText("""
            package test
            val icon1 = "▲"
            val icon2 = "🔒"
        """.trimIndent())

        val violations = StyleGuard.scan(mainDir.parentFile.parentFile)
        assertEquals(2, violations.size)
        assertTrue(violations[0].rule.contains("Rule 32a"))
        assertTrue(violations[1].rule.contains("Rule 32a"))
    }

    @Test
    fun flagsMonospaceInMainSource() {
        val mainDir = tempFolder.newFolder("src", "main", "java")
        val file = tempFolder.newFile("src/main/java/Screen.kt")
        file.writeText("""
            package test
            import androidx.compose.ui.text.font.FontFamily
            val font = FontFamily.Monospace
        """.trimIndent())

        val violations = StyleGuard.scan(mainDir.parentFile.parentFile)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("Rule 32b"))
    }

    @Test
    fun flagsUppercaseInTextInMainSource() {
        val mainDir = tempFolder.newFolder("src", "main", "java")
        val file = tempFolder.newFile("src/main/java/Screen.kt")
        file.writeText("""
            package test
            fun MyView(title: String) {
                Text(text = title.uppercase())
            }
        """.trimIndent())

        val violations = StyleGuard.scan(mainDir.parentFile.parentFile)
        assertEquals(1, violations.size)
        assertTrue(violations[0].rule.contains("Rule 32b"))
    }

    @Test
    fun respectsAllowlist() {
        val mainDir = tempFolder.newFolder("src", "main", "java")
        val file = tempFolder.newFile("src/main/java/Screen.kt")
        file.writeText("""
            package test
            val icon = "▲"
        """.trimIndent())

        val allowlist = tempFolder.newFile("allowlist.txt")
        allowlist.writeText("src/main/java/Screen.kt:2\n")

        val violations = StyleGuard.scan(mainDir.parentFile.parentFile, allowlist)
        assertEquals(0, violations.size)
    }
}
