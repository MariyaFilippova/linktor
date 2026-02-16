package io.github.linktor

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * The result of checking a single URL.
 */
data class LinkResult(
    val url: String,
    val statusCode: Int?,          // null if timeout/connection error
    val responseTimeMs: Long,
    val redirectChain: List<String>, // empty if no redirects
    val error: String?,             // null if successful
    val linkedFrom: MutableSet<String> = ConcurrentHashMap.newKeySet(),
) {
    val isBroken: Boolean
        get() = statusCode == null || statusCode in 400..599

    val isRedirect: Boolean
        get() = redirectChain.size > 1

    val isWarning: Boolean
        get() = isRedirect && redirectChain.size >= 3
}

/**
 * Final crawl report.
 */
data class CrawlReport(
    val baseUrl: String,
    val results: List<LinkResult>,
    val totalTimeMs: Long,
    val pagesScanned: Int,
    val externalLinksChecked: Int,
    val timedOut: Boolean = false,
) {
    val broken: List<LinkResult> by lazy { results.filter { it.isBroken } }
    val warnings: List<LinkResult> by lazy { results.filter { it.isWarning && !it.isBroken } }
    val ok: List<LinkResult> by lazy { results.filter { !it.isBroken && !it.isWarning } }
}

/**
 * Configuration for a crawl run.
 */
data class CrawlConfig(
    val baseUrl: String,
    val maxDepth: Int = 5,
    val concurrency: Int = 15,
    val timeoutMs: Long = 5000,
    val checkExternal: Boolean = false,
    val ignorePaths: List<String> = emptyList(),
    val verbose: Boolean = false,
    val maxCrawlTimeMs: Long = 0,
) {
    val baseHost: String = URI(baseUrl).host

    fun isInternal(url: String): Boolean {
        return try {
            URI(url).host == baseHost
        } catch (e: Exception) {
            false
        }
    }

    private val ignorePatterns: List<Regex> by lazy {
        ignorePaths.map { pattern ->
            pattern
                .split("*")
                .joinToString(".*") { Regex.escape(it) }
                .toRegex()
        }
    }

    fun isIgnored(url: String): Boolean {
        return ignorePatterns.any { it.containsMatchIn(url) }
    }
}
