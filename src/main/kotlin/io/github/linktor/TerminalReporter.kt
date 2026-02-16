package io.github.linktor

private const val RED = "\u001b[31m"
private const val YELLOW = "\u001b[33m"
private const val GREEN = "\u001b[32m"
private const val BOLD = "\u001b[1m"
private const val DIM = "\u001b[2m"
private const val RESET = "\u001b[0m"

object TerminalReporter {
    fun print(report: CrawlReport) {
        println()
        println("${BOLD}🔗 linktor — ${report.baseUrl}${RESET}")
        println(
            "$DIM   Scanned: ${report.pagesScanned} internal, " +
                    "${report.externalLinksChecked} external  |  " +
                    "Time: ${"%.1f".format(report.totalTimeMs / 1000.0)}s${RESET}"
        )
        println()

        // Broken links
        for (result in report.broken) {
            val status = result.statusCode?.toString() ?: "TIMEOUT"
            println(" ${RED}✗ $status${RESET}  ${result.url}")
            if (result.error != null) {
                println("   ${DIM}error: ${result.error}${RESET}")
            }
            for (source in result.linkedFrom.filter { it != "seed" }) {
                println("   ${DIM}← $source${RESET}")
            }
            println()
        }

        // Warnings (long redirect chains)
        for (result in report.warnings) {
            val chain = result.redirectChain.joinToString(" → ") { shortenUrl(it) }
            println(" ${YELLOW}⚠ ${chain}${RESET}")
            println("   ${DIM}redirect chain (${result.redirectChain.size} hops)${RESET}")
            for (source in result.linkedFrom.filter { it != "seed" }) {
                println("   ${DIM}← $source${RESET}")
            }
            println()
        }

        val brokenCount = report.broken.size
        val warningCount = report.warnings.size
        val okCount = report.ok.size

        if (report.timedOut) {
            println(" ${YELLOW}⚠ Crawl timed out — results are partial${RESET}")
            println()
        }

        println(
            "$BOLD Summary: " +
                    "${RED}$brokenCount broken${RESET}${BOLD} | " +
                    "${YELLOW}$warningCount warnings${RESET}${BOLD} | " +
                    "${GREEN}$okCount ok${RESET}"
        )
        println()
    }

    private fun shortenUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.path ?: url
        } catch (e: Exception) {
            url
        }
    }
}