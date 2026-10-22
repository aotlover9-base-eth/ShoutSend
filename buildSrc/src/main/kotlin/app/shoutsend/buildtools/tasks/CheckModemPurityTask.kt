package app.shoutsend.buildtools.tasks

import app.shoutsend.buildtools.ModemPurityGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

abstract class CheckModemPurityTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    init {
        group = "verification"
        description = "Verifies that :modem contains 0 android.* or androidx.* imports."
    }

    @TaskAction
    fun execute() {
        val dir = sourceDir.get().asFile
        val guard = ModemPurityGuard()
        val violations = guard.scan(dir)

        if (violations.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("checkModemPurity: Found ${violations.size} forbidden Android dependencies in :modem:\n")
            violations.forEach { v ->
                sb.append("  - ${v.file.path}:${v.line}: ${v.message} -> ${v.content}\n")
            }
            throw GradleException(sb.toString())
        }
        logger.lifecycle("checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).")
    }
}
