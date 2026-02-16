package io.github.linktor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkResultTest {

    @Test
    fun `isBroken when statusCode is null`() {
        val result = linkResult(statusCode = null, error = "timeout")
        assertTrue(result.isBroken)
    }

    @Test
    fun `isBroken for 404`() {
        val result = linkResult(statusCode = 404)
        assertTrue(result.isBroken)
    }

    @Test
    fun `isBroken for 500`() {
        val result = linkResult(statusCode = 500)
        assertTrue(result.isBroken)
    }

    @Test
    fun `not broken for 200`() {
        val result = linkResult(statusCode = 200)
        assertFalse(result.isBroken)
    }

    @Test
    fun `not broken for 301`() {
        val result = linkResult(statusCode = 301, redirectChain = listOf("a", "b"))
        assertFalse(result.isBroken)
    }

    @Test
    fun `isRedirect when chain has more than one entry`() {
        val result = linkResult(statusCode = 200, redirectChain = listOf("a", "b"))
        assertTrue(result.isRedirect)
    }

    @Test
    fun `not redirect when chain has one entry`() {
        val result = linkResult(statusCode = 200, redirectChain = listOf("a"))
        assertFalse(result.isRedirect)
    }

    @Test
    fun `isWarning for long redirect chain`() {
        val result = linkResult(statusCode = 200, redirectChain = listOf("a", "b", "c"))
        assertTrue(result.isWarning)
    }

    @Test
    fun `not warning for short redirect chain`() {
        val result = linkResult(statusCode = 200, redirectChain = listOf("a", "b"))
        assertFalse(result.isWarning)
    }

    private fun linkResult(
        statusCode: Int? = 200,
        redirectChain: List<String> = emptyList(),
        error: String? = null,
    ) = LinkResult(
        url = "https://example.com",
        statusCode = statusCode,
        responseTimeMs = 100,
        redirectChain = redirectChain,
        error = error,
    )
}

class CrawlConfigTest {

    private val config = CrawlConfig(baseUrl = "https://example.com")

    @Test
    fun `baseHost is extracted from URL`() {
        assertEquals("example.com", config.baseHost)
    }

    @Test
    fun `isInternal returns true for same host`() {
        assertTrue(config.isInternal("https://example.com/page"))
    }

    @Test
    fun `isInternal returns false for different host`() {
        assertFalse(config.isInternal("https://other.com/page"))
    }

    @Test
    fun `isInternal returns false for invalid URL`() {
        assertFalse(config.isInternal("not a url"))
    }

    @Test
    fun `isIgnored matches exact path`() {
        val cfg = CrawlConfig(baseUrl = "https://example.com", ignorePaths = listOf("/admin"))
        assertTrue(cfg.isIgnored("https://example.com/admin"))
    }

    @Test
    fun `isIgnored matches wildcard pattern`() {
        val cfg = CrawlConfig(baseUrl = "https://example.com", ignorePaths = listOf("/api/*"))
        assertTrue(cfg.isIgnored("https://example.com/api/v1/users"))
    }

    @Test
    fun `isIgnored returns false for non-matching URL`() {
        val cfg = CrawlConfig(baseUrl = "https://example.com", ignorePaths = listOf("/admin"))
        assertFalse(cfg.isIgnored("https://example.com/page"))
    }

    @Test
    fun `isIgnored with no patterns returns false`() {
        assertFalse(config.isIgnored("https://example.com/anything"))
    }
}

class CrawlReportTest {

    @Test
    fun `broken filters broken results`() {
        val report = reportWith(
            linkResult(200),
            linkResult(404),
            linkResult(null, error = "timeout"),
        )
        assertEquals(2, report.broken.size)
    }

    @Test
    fun `warnings filters warning results`() {
        val report = reportWith(
            linkResult(200),
            linkResult(200, redirectChain = listOf("a", "b", "c")),
        )
        assertEquals(1, report.warnings.size)
    }

    @Test
    fun `ok filters non-broken non-warning results`() {
        val report = reportWith(
            linkResult(200),
            linkResult(404),
            linkResult(200, redirectChain = listOf("a", "b", "c")),
        )
        assertEquals(1, report.ok.size)
        assertEquals(200, report.ok[0].statusCode)
        assertEquals(emptyList(), report.ok[0].redirectChain)
    }

    private fun linkResult(
        statusCode: Int?,
        redirectChain: List<String> = emptyList(),
        error: String? = null,
    ) = LinkResult(
        url = "https://example.com/${statusCode ?: "null"}-${redirectChain.size}",
        statusCode = statusCode,
        responseTimeMs = 50,
        redirectChain = redirectChain,
        error = error,
    )

    private fun reportWith(vararg results: LinkResult) = CrawlReport(
        baseUrl = "https://example.com",
        results = results.toList(),
        totalTimeMs = 1000,
        pagesScanned = results.size,
        externalLinksChecked = 0,
    )
}
