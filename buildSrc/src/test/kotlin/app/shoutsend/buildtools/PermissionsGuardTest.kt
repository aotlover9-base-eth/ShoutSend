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
    fun hardFailsOnCameraPermission() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.CAMERA" />
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
    fun passesWithAllowedV2Permissions() {
        val manifest = tempFolder.newFile("AndroidManifest.xml")
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.RECORD_AUDIO" />
                <uses-permission android:name="android.permission.VIBRATE" />
                <uses-permission android:name="android.permission.INTERNET" />
                <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
                <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
                <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:usesPermissionFlags="neverForLocation" />
                <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="32" />
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
                <uses-permission android:name="android.permission.READ_PHONE_STATE" />
            </manifest>
        """.trimIndent())

        val violations = PermissionsGuard.scan(manifest)
        assertEquals(1, violations.size)
        assertTrue(violations[0].reason.contains("allowed permission set"))
    }
}
