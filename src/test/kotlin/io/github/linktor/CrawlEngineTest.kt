package io.github.linktor

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeHttpFetcher(
    private val responses: Map<String, Pair<LinkResult, String?>>,
) : HttpFetcher {
    private val defaultResponse: (String) -> Pair<LinkResult, String?> = { url ->
        Pair(
            LinkResult(url = url, statusCode = null, responseTimeMs = 0, redirectChain = listOf(url), error = "Not found in fake"),
            null
        )
    }

    override suspend fun fetch(url: String): Pair<LinkResult, String?> {
        return responses[url] ?: defaultResponse(url)
    }

    override fun close() {}
}

private fun ok(url: String, body: String? = null) = Pair(
    LinkResult(url = url, statusCode = 200, responseTimeMs = 10, redirectChain = listOf(url), error = null),
    body
)

private fun broken(url: String, statusCode: Int = 404) = Pair(
    LinkResult(url = url, statusCode = statusCode, responseTimeMs = 10, redirectChain = listOf(url), error = null),
    null
)

private fun error(url: String, message: String = "Connection refused") = Pair(
    LinkResult(url = url, statusCode = null, responseTimeMs = 10, redirectChain = listOf(url), error = message),
    null
)

class CrawlEngineTest {

    @Test
    fun `crawls single page with no links`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok("https://example.com", "<html><body>Hello</body></html>")
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(1, report.pagesScanned)
        assertEquals(0, report.broken.size)
    }

    @Test
    fun `discovers and follows internal links`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://example.com/about">about</a></body></html>"""
                ),
                "https://example.com/about" to ok(
                    "https://example.com/about",
                    "<html><body>About page</body></html>"
                ),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(2, report.pagesScanned)
        assertTrue(report.results.any { it.url == "https://example.com/about" })
    }

    @Test
    fun `detects broken links`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://example.com/missing">missing</a></body></html>"""
                ),
                "https://example.com/missing" to broken("https://example.com/missing"),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(1, report.broken.size)
        assertEquals("https://example.com/missing", report.broken[0].url)
    }

    @Test
    fun `respects maxDepth`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://example.com/level1">l1</a></body></html>"""
                ),
                "https://example.com/level1" to ok(
                    "https://example.com/level1",
                    """<html><body><a href="https://example.com/level2">l2</a></body></html>"""
                ),
                "https://example.com/level2" to ok(
                    "https://example.com/level2",
                    """<html><body><a href="https://example.com/level3">l3</a></body></html>"""
                ),
                "https://example.com/level3" to ok(
                    "https://example.com/level3",
                    "<html><body>deep</body></html>"
                ),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", maxDepth = 1, concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        // depth 0 = base, level1 is queued at depth 1
        // level1 at depth 1: depth < maxDepth (1 < 1) is false, so its links aren't followed
        assertTrue(report.results.any { it.url == "https://example.com/level1" })
        // level2 is never queued because level1's links aren't followed
        assertTrue(report.results.none { it.url == "https://example.com/level2" })
        assertTrue(report.results.none { it.url == "https://example.com/level3" })
    }

    @Test
    fun `skips external links when checkExternal is false`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://other.com/page">external</a></body></html>"""
                ),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", checkExternal = false, concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(0, report.externalLinksChecked)
    }

    @Test
    fun `checks external links when checkExternal is true`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://other.com/page">external</a></body></html>"""
                ),
                "https://other.com/page" to ok("https://other.com/page"),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", checkExternal = true, concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(1, report.externalLinksChecked)
    }

    @Test
    fun `ignores URLs matching ignorePaths`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://example.com/admin/settings">admin</a></body></html>"""
                ),
            )
        )
        val config = CrawlConfig(
            baseUrl = "https://example.com",
            ignorePaths = listOf("/admin/*"),
            concurrency = 1,
        )
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertTrue(report.results.none { it.url == "https://example.com/admin/settings" })
    }

    @Test
    fun `handles fetch errors gracefully`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body><a href="https://example.com/fail">fail</a></body></html>"""
                ),
                "https://example.com/fail" to error("https://example.com/fail"),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertEquals(1, report.broken.size)
        assertEquals("Connection refused", report.broken[0].error)
    }

    @Test
    fun `does not visit same URL twice`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf(
                "https://example.com" to ok(
                    "https://example.com",
                    """<html><body>
                        <a href="https://example.com/a">a</a>
                        <a href="https://example.com/shared">shared</a>
                    </body></html>"""
                ),
                "https://example.com/a" to ok(
                    "https://example.com/a",
                    """<html><body><a href="https://example.com/shared">shared</a></body></html>"""
                ),
                "https://example.com/shared" to ok(
                    "https://example.com/shared",
                    "<html><body>shared page</body></html>"
                ),
            )
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        val sharedResults = report.results.filter { it.url == "https://example.com/shared" }
        assertEquals(1, sharedResults.size)
    }

    @Test
    fun `crawl times out and returns partial results`() = runTest {
        val slowFetcher = object : HttpFetcher {
            override suspend fun fetch(url: String): Pair<LinkResult, String?> {
                if (url != "https://example.com") {
                    delay(10_000) // very slow for child pages
                }
                val body = """<html><body><a href="https://example.com/slow">slow</a></body></html>"""
                return ok(url, body)
            }
            override fun close() {}
        }
        val config = CrawlConfig(
            baseUrl = "https://example.com",
            concurrency = 1,
            maxCrawlTimeMs = 200,
        )
        val engine = CrawlEngine(config, slowFetcher)
        val report = engine.crawl()

        assertTrue(report.timedOut)
        // The base page was fetched before timeout
        assertTrue(report.results.any { it.url == "https://example.com" })
    }

    @Test
    fun `timedOut is false when crawl completes normally`() = runTest {
        val fetcher = FakeHttpFetcher(
            mapOf("https://example.com" to ok("https://example.com", "<html><body>Hello</body></html>"))
        )
        val config = CrawlConfig(baseUrl = "https://example.com", concurrency = 1)
        val engine = CrawlEngine(config, fetcher)
        val report = engine.crawl()

        assertFalse(report.timedOut)
    }
}
