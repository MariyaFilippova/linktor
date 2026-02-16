package io.github.linktor

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class CrawlEngine(
    private val config: CrawlConfig,
    private val fetcher: HttpFetcher = KtorHttpFetcher(config.timeoutMs),
) {
    private val semaphore = Semaphore(config.concurrency)

    private val visited = ConcurrentHashMap.newKeySet<String>()
    private val results = ConcurrentHashMap<String, LinkResult>()

    suspend fun crawl(): CrawlReport {
        var timedOut = false
        val time = measureTime {
            fetcher.use {
                val completed = if (config.maxCrawlTimeMs > 0) {
                    withTimeoutOrNull(config.maxCrawlTimeMs.milliseconds) {
                        doCrawl()
                    }
                } else {
                    doCrawl()
                }
                if (completed == null) timedOut = true
            }
        }

        return CrawlReport(
            baseUrl = config.baseUrl,
            results = results.values.toList(),
            totalTimeMs = time.inWholeMilliseconds,
            pagesScanned = results.count { config.isInternal(it.key) },
            externalLinksChecked = results.count { !config.isInternal(it.key) },
            timedOut = timedOut,
        )
    }

    private suspend fun doCrawl() = coroutineScope {
        visited.add(config.baseUrl)
        launch { processUrl(config.baseUrl, "seed", 0, this@coroutineScope) }
    }

    private suspend fun processUrl(
        url: String,
        sourceUrl: String,
        depth: Int,
        scope: CoroutineScope,
    ) {
        val isExternal = !config.isInternal(url)
        if (isExternal && !config.checkExternal) return
        if (config.isIgnored(url)) return

        if (config.verbose) {
            println("  → $url (depth=$depth)")
        }

        val (result, body) = semaphore.withPermit {
            fetcher.fetch(url)
        }

        result.linkedFrom.add(sourceUrl)

        results.merge(url, result) { existing, new ->
            existing.linkedFrom.addAll(new.linkedFrom)
            existing
        }

        if (!isExternal && !result.isBroken && depth < config.maxDepth && body != null) {
            val links = LinkExtractor.extract(body, url)
            for (link in links) {
                if (visited.add(link)) {
                    scope.launch {
                        processUrl(link, url, depth + 1, scope)
                    }
                }
            }
        }
    }
}
