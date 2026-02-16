package io.github.linktor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class LinkExtractorTest {

    @Test
    fun `extracts absolute links`() {
        val html = """<html><body><a href="https://example.com/page">link</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(listOf("https://example.com/page"), links)
    }

    @Test
    fun `resolves relative links to absolute`() {
        val html = """<html><body><a href="/about">about</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com/")
        assertEquals(listOf("https://example.com/about"), links)
    }

    @Test
    fun `filters out mailto links`() {
        val html = """<html><body><a href="mailto:test@example.com">email</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }

    @Test
    fun `filters out javascript links`() {
        val html = """<html><body><a href="javascript:void(0)">js</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }

    @Test
    fun `filters out tel links`() {
        val html = """<html><body><a href="tel:+1234567890">call</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }

    @Test
    fun `filters out fragment-only links`() {
        val html = """<html><body><a href="#section">section</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }

    @Test
    fun `strips fragment from URLs`() {
        val html = """<html><body><a href="https://example.com/page#section">link</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(listOf("https://example.com/page"), links)
    }

    @Test
    fun `deduplicates links`() {
        val html = """
            <html><body>
                <a href="https://example.com/page">link1</a>
                <a href="https://example.com/page">link2</a>
            </body></html>
        """.trimIndent()
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(listOf("https://example.com/page"), links)
    }

    @Test
    fun `deduplicates after fragment stripping`() {
        val html = """
            <html><body>
                <a href="https://example.com/page#a">link1</a>
                <a href="https://example.com/page#b">link2</a>
            </body></html>
        """.trimIndent()
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(listOf("https://example.com/page"), links)
    }

    @Test
    fun `extracts multiple different links`() {
        val html = """
            <html><body>
                <a href="https://example.com/a">a</a>
                <a href="https://example.com/b">b</a>
                <a href="https://other.com/c">c</a>
            </body></html>
        """.trimIndent()
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(3, links.size)
        assertContains(links, "https://example.com/a")
        assertContains(links, "https://example.com/b")
        assertContains(links, "https://other.com/c")
    }

    @Test
    fun `returns empty list for HTML with no links`() {
        val html = """<html><body><p>No links here</p></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }

    @Test
    fun `ignores anchors without href`() {
        val html = """<html><body><a name="top">anchor</a></body></html>"""
        val links = LinkExtractor.extract(html, "https://example.com")
        assertEquals(emptyList(), links)
    }
}
