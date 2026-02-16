package io.github.linktor

open class LinktorExtension {
    var url: String = ""
    var maxDepth: Int = 5
    var concurrency: Int = 15
    var timeoutMs: Long = 5000
    var checkExternal: Boolean = false
    var ignorePaths: List<String> = emptyList()
    var maxCrawlTimeMs: Long = 0
    var failOnBroken: Boolean = false
}
