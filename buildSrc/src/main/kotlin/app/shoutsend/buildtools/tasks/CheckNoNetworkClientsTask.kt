package app.shoutsend.buildtools.tasks

import app.shoutsend.buildtools.NetworkClientsGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckNoNetworkClientsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirs: ConfigurableFileCollection

    init {
        group = "verification"
        description = "Enforces Rule 29: fails build if network clients (HTTP, OkHttp, Ktor, Retrofit, WebView, URL I/O) are found."
    }

    @TaskAction
    fun execute() {
        val dirs = sourceDirs.files.toList()
        if (dirs.isEmpty()) {
            logger.warn("CheckNoNetworkClientsTask: No source directories specified.")
            return
        }

        val violations = NetworkClientsGuard.scan(dirs)
        if (violations.isNotEmpty()) {
            val report = buildString {
                appendLine("CheckNoNetworkClientsTask: Found ${violations.size} network client violation(s) violating Rule 29:")
                violations.forEach { appendLine("  - $it") }
            }
            throw GradleException(report)
        }

        logger.lifecycle("checkNoNetworkClients: Clean! Zero network client libraries or outbound HTTP callers found.")
    }
}
