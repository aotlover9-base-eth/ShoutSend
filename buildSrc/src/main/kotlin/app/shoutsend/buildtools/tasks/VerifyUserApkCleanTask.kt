package app.shoutsend.buildtools.tasks

import app.shoutsend.buildtools.ApkPurityGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyUserApkCleanTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val markersFile: RegularFileProperty

    init {
        group = "verification"
        description = "Verifies that the User APK contains zero dev-only classes or marker tokens."
    }

    @TaskAction
    fun execute() {
        val apk = apkFile.asFile.get()
        val markers = markersFile.asFile.get()

        val violations = ApkPurityGuard.scan(apk, markers)
        if (violations.isNotEmpty()) {
            val report = buildString {
                appendLine("VerifyUserApkClean: Found ${violations.size} forbidden dev marker(s) in User APK ${apk.name}:")
                violations.forEach { appendLine("  - $it") }
            }
            throw GradleException(report)
        }
        logger.lifecycle("verifyUserApkClean: User APK ${apk.name} is 100% clean of dev tools.")
    }
}
