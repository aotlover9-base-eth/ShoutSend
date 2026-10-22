package app.shoutsend.buildtools.tasks

import app.shoutsend.buildtools.PermissionsGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyPermissionsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFiles: ConfigurableFileCollection

    init {
        group = "verification"
        description = "Verifies that merged manifests strictly adhere to offline permissions policy (no INTERNET, only audio/vibrate)."
    }

    @TaskAction
    fun execute() {
        val files = manifestFiles.files
        if (files.isEmpty()) {
            logger.warn("VerifyPermissionsTask: No merged manifest files found to verify.")
            return
        }

        val allViolations = mutableListOf<String>()
        for (manifest in files) {
            val violations = PermissionsGuard.scan(manifest)
            violations.forEach { allViolations.add(it.toString()) }
        }

        if (allViolations.isNotEmpty()) {
            val report = buildString {
                appendLine("VerifyPermissionsTask: Found ${allViolations.size} permission violation(s):")
                allViolations.forEach { appendLine("  - $it") }
            }
            throw GradleException(report)
        }
        logger.lifecycle("verifyPermissions: Merged manifests comply with strict offline permissions policy.")
    }
}
