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
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
    )

    val ALLOWED_PERMISSIONS = setOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.VIBRATE"
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
                        reason = "is STRICTLY FORBIDDEN by Rule 24. ShoutSend is 100% offline acoustic software."
                    )
                )
            } else if (perm !in ALLOWED_PERMISSIONS && !perm.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")) {
                violations.add(
                    PermissionViolation(
                        manifestFile = manifestFile,
                        permission = perm,
                        isHardForbidden = false,
                        reason = "is not in the allowed permission whitelist [RECORD_AUDIO, VIBRATE, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION]."
                    )
                )
            }
        }

        return violations
    }
}
