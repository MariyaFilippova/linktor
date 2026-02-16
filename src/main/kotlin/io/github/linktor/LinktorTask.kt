package io.github.linktor

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class LinktorTask : DefaultTask() {

    init {
        group = "verification"
        description = "Checks for broken links on a website"
    }

    @get:Input
    var url: String = ""

    @get:Input
    var maxDepth: Int = 5

    @get:Input
    var concurrency: Int = 15

    @get:Input
    var timeoutMs: Long = 5000

    @get:Input
    var checkExternal: Boolean = false

    @get:Input
    var ignorePaths: List<String> = emptyList()

    @get:Input
    var maxCrawlTimeMs: Long = 0

    @get:Input
    var failOnBroken: Boolean = false

    @TaskAction
    fun run() {
        require(url.isNotBlank()) { "linktor: 'url' must be set in the linktor extension" }

        val config = CrawlConfig(
            baseUrl = url,
            maxDepth = maxDepth,
            concurrency = concurrency,
            timeoutMs = timeoutMs,
            checkExternal = checkExternal,
            ignorePaths = ignorePaths,
            maxCrawlTimeMs = maxCrawlTimeMs,
        )

        val engine = CrawlEngine(config)
        val report = runBlocking { engine.crawl() }

        TerminalReporter.print(report)

        if (failOnBroken && report.broken.isNotEmpty()) {
            throw GradleException("linktor: found ${report.broken.size} broken link(s)")
        }
    }
}
