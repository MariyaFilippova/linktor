package io.github.linktor

import org.jsoup.Jsoup

/**
 * Parses HTML and extracts all links from a page.
 * Normalizes relative URLs to absolute ones.
 */
object LinkExtractor {
    fun extract(html: String, pageUrl: String): List<String> {
        val document = Jsoup.parse(html, pageUrl)

        return document.select("a[href]")
            .mapNotNull { element ->
                val rawHref = element.attr("href")
                if (rawHref.startsWith("#")) return@mapNotNull null

                val href = element.attr("abs:href")

                if (href.isBlank() ||
                    href.startsWith("mailto:") ||
                    href.startsWith("javascript:") ||
                    href.startsWith("tel:")
                ) {
                    return@mapNotNull null
                }

                href.substringBefore("#")
            }
            .distinct()
    }
}
