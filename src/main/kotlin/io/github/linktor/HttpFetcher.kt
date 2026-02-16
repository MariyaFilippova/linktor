package io.github.linktor

import java.io.Closeable

interface HttpFetcher : Closeable {
    suspend fun fetch(url: String): Pair<LinkResult, String?>
}
