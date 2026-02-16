package io.github.linktor

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class TerminalReporterTest {

    @Test
    fun `prints summary counts`() {
        val report = CrawlReport(
            baseUrl = "https://example.com",
            results = listOf(
                linkResult(200),
                linkResult(404),
            ),
            totalTimeMs = 1234,
            pagesScanned = 2,
            externalLinksChecked = 0,
        )
        val output = captureStdout { TerminalReporter.print(report) }

        assertContains(output, "1 broken")
        assertContains(output, "1 ok")
    }

    @Test
    fun `prints broken link details`() {
        val broken = LinkResult(
            url = "https://example.com/missing",
            statusCode = 404,
            responseTimeMs = 100,
            redirectChain = listOf("https://example.com/missing"),
            error = null,
        )
        broken.linkedFrom.add("https://example.com/")
        val report = CrawlReport(
            baseUrl = "https://example.com",
            results = listOf(broken),
            totalTimeMs = 500,
            pagesScanned = 1,
            externalLinksChecked = 0,
        )
        val output = captureStdout { TerminalReporter.print(report) }

        assertContains(output, "404")
        assertContains(output, "https://example.com/missing")
        assertContains(output, "https://example.com/")
    }

    @Test
    fun `prints TIMEOUT for null status code`() {
        val broken = LinkResult(
            url = "https://example.com/timeout",
            statusCode = null,
            responseTimeMs = 5000,
            redirectChain = listOf("https://example.com/timeout"),
            error = "Connection timed out",
        )
        val report = CrawlReport(
            baseUrl = "https://example.com",
            results = listOf(broken),
            totalTimeMs = 5000,
            pagesScanned = 1,
            externalLinksChecked = 0,
        )
        val output = captureStdout { TerminalReporter.print(report) }

        assertContains(output, "TIMEOUT")
        assertContains(output, "Connection timed out")
    }

    @Test
    fun `prints warning for long redirect chain`() {
        val warning = LinkResult(
            url = "https://example.com/old",
            statusCode = 200,
            responseTimeMs = 300,
            redirectChain = listOf(
                "https://example.com/old",
                "https://example.com/middle",
                "https://example.com/new",
            ),
            error = null,
        )
        val report = CrawlReport(
            baseUrl = "https://example.com",
            results = listOf(warning),
            totalTimeMs = 300,
            pagesScanned = 1,
            externalLinksChecked = 0,
        )
        val output = captureStdout { TerminalReporter.print(report) }

        assertContains(output, "1 warnings")
        assertContains(output, "redirect chain")
        assertContains(output, "3 hops")
    }

    @Test
    fun `handles empty report`() {
        val report = CrawlReport(
            baseUrl = "https://example.com",
            results = emptyList(),
            totalTimeMs = 100,
            pagesScanned = 0,
            externalLinksChecked = 0,
        )
        val output = captureStdout { TerminalReporter.print(report) }

        assertContains(output, "0 broken")
        assertContains(output, "0 ok")
        // Just verify it doesn't crash
        assertTrue(output.isNotEmpty())
    }

    private fun linkResult(statusCode: Int?) = LinkResult(
        url = "https://example.com/${statusCode ?: "null"}",
        statusCode = statusCode,
        responseTimeMs = 50,
        redirectChain = if (statusCode != null) listOf("https://example.com/$statusCode") else emptyList(),
        error = if (statusCode == null) "error" else null,
    )

    private fun captureStdout(block: () -> Unit): String {
        val baos = ByteArrayOutputStream()
        val old = System.out
        System.setOut(PrintStream(baos))
        try {
            block()
        } finally {
            System.setOut(old)
        }
        return baos.toString()
    }
}
