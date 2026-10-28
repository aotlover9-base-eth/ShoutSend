package app.shoutsend.buildtools

import java.io.File

data class PermissionViolation(
    val manifestFile: File,
    val permission: String,
    val isHardForbidden: Boolean,
    val reason: String
) {
    override fun toString(): String = "${manifestFile.path}: permission '$permission' $reason"
}

object PermissionsGuard {

    private val PERMISSION_REGEX = Regex("""<uses-permission\s+[^>]*android:name\s*=\s*"([^"]+)"""")

    val HARD_FORBIDDEN_PERMISSIONS = setOf(
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.CAMERA",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.FOREGROUND_SERVICE"
    )

    val ALLOWED_PERMISSIONS = setOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.VIBRATE",
        "android.permission.INTERNET",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    fun scan(manifestFile: File): List<PermissionViolation> {
        val violations = mutableListOf<PermissionViolation>()
        if (!manifestFile.exists()) {
            throw IllegalArgumentException("Manifest file not found: ${manifestFile.absolutePath}")
        }

        val content = manifestFile.readText()
        val permissionsFound = PERMISSION_REGEX.findAll(content).map { it.groupValues[1] }.toSet()

        for (perm in permissionsFound) {
            if (perm in HARD_FORBIDDEN_PERMISSIONS) {
                violations.add(
                    PermissionViolation(
                        manifestFile = manifestFile,
                        permission = perm,
                        isHardForbidden = true,
                        reason = "is STRICTLY FORBIDDEN by Rule 24 (replaced in v2)."
                    )
                )
            } else if (perm !in ALLOWED_PERMISSIONS && !perm.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")) {
                violations.add(
                    PermissionViolation(
                        manifestFile = manifestFile,
                        permission = perm,
                        isHardForbidden = false,
                        reason = "is not in the allowed permission set from Rule 24 (replaced in v2) [RECORD_AUDIO, VIBRATE, INTERNET, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, NEARBY_WIFI_DEVICES, ACCESS_FINE_LOCATION, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION]."
                    )
                )
            }
        }

        return violations
    }
}
