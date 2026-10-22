package app.shoutsend.buildtools.tasks

import app.shoutsend.buildtools.StyleGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckNoHardcodedStyleTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allowlistFile: RegularFileProperty

    init {
        group = "verification"
        description = "Verifies that no raw hex colors, named color constants, shape literals, or dp/sp literals leak into UI code."
    }

    @TaskAction
    fun execute() {
        val src = sourceDir.asFile.get()
        val allowlist = if (allowlistFile.isPresent) allowlistFile.asFile.get() else null

        val violations = StyleGuard.scan(src, allowlist)
        if (violations.isNotEmpty()) {
            val report = buildString {
                appendLine("StyleGuard found ${violations.size} rule violation(s):")
                violations.forEach { appendLine("  - $it") }
            }
            throw GradleException(report)
        }
        logger.lifecycle("checkNoHardcodedStyle: All sources comply with tokenization rules.")
    }
}
