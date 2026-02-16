package io.github.linktor

import org.gradle.api.Plugin
import org.gradle.api.Project

class LinktorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("linktor", LinktorExtension::class.java)

        project.tasks.register("checkLinks", LinktorTask::class.java) { task ->
            task.url = extension.url
            task.maxDepth = extension.maxDepth
            task.concurrency = extension.concurrency
            task.timeoutMs = extension.timeoutMs
            task.checkExternal = extension.checkExternal
            task.ignorePaths = extension.ignorePaths
            task.maxCrawlTimeMs = extension.maxCrawlTimeMs
            task.failOnBroken = extension.failOnBroken
        }
    }
}
