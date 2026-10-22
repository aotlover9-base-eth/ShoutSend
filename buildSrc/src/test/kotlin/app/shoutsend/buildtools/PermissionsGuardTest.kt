package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PermissionsGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun hardFailsOnInternetPermission() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.INTERNET" />
            </manifest>
        """.trimIndent())

        val violations = PermissionsGuard.scan(manifest)
        assertEquals(1, violations.size)
        assertTrue(violations[0].isHardForbidden)
        assertTrue(violations[0].reason.contains("Rule 24"))
    }

    @Test
    fun hardFailsOnAccessNetworkState() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
            </manifest>
        """.trimIndent())

        val violations = PermissionsGuard.scan(manifest)
        assertEquals(1, violations.size)
        assertTrue(violations[0].isHardForbidden)
    }

    @Test
    fun passesWithAllowedPermissionsOnly() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.RECORD_AUDIO" />
                <uses-permission android:name="android.permission.VIBRATE" />
                <uses-permission android:name="app.shoutsend.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
            </manifest>
        """.trimIndent())

        val violations = PermissionsGuard.scan(manifest)
        assertEquals(0, violations.size)
    }

    @Test
    fun catchesUnknownPermissions() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.CAMERA" />
            </manifest>
        """.trimIndent())

        val violations = PermissionsGuard.scan(manifest)
        assertEquals(1, violations.size)
        assertTrue(violations[0].reason.contains("whitelist"))
    }
}
