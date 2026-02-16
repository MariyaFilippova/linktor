package io.github.linktor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import kotlinx.coroutines.runBlocking

class LinktorCommand : CliktCommand(
    name = "linktor",
    help = "🔗 Fast, concurrent broken link checker"
) {
    private val url by argument(help = "URL to crawl")

    private val depth by option("--depth", "-d", help = "Max crawl depth")
        .int().default(5)

    private val concurrency by option("--concurrency", "-c", help = "Max concurrent requests")
        .int().default(15)

    private val timeout by option("--timeout", "-t", help = "Request timeout in ms")
        .long().default(5000)

    private val checkExternal by option("--check-external", "-e", help = "Also check external links")
        .flag(default = false)

    private val verbose by option("--verbose", "-v", help = "Verbose output")
        .flag(default = false)

    private val maxTime by option("--max-time", "-m", help = "Max total crawl time in ms (0 = unlimited)")
        .long().default(0)

    private val failOnBroken by option("--fail-on-broken", "-f", help = "Exit with code 1 if broken links found")
        .flag(default = false)

    override fun run() {
        val config = CrawlConfig(
            baseUrl = url,
            maxDepth = depth,
            concurrency = concurrency,
            timeoutMs = timeout,
            checkExternal = checkExternal,
            verbose = verbose,
            maxCrawlTimeMs = maxTime,
        )

        val engine = CrawlEngine(config)

        val report = runBlocking {
            engine.crawl()
        }

        TerminalReporter.print(report)

        if (failOnBroken && report.broken.isNotEmpty()) {
            throw ProgramResult(1)
        }
    }
}

fun main(args: Array<String>) = LinktorCommand().main(args)
