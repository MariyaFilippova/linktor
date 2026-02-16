package io.github.linktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class KtorHttpFetcher(timeoutMs: Long) : HttpFetcher {
    private val client = HttpClient(CIO) {
        followRedirects = false
        engine {
            requestTimeout = timeoutMs
        }
    }

    override suspend fun fetch(url: String): Pair<LinkResult, String?> {
        val start = System.currentTimeMillis()
        val redirectChain = mutableListOf(url)

        return try {
            var currentUrl = url
            var response: HttpResponse

            do {
                response = client.get(currentUrl)
                if (response.status.isRedirect()) {
                    val location = response.headers[HttpHeaders.Location]
                        ?: break
                    currentUrl = resolveRedirect(currentUrl, location)
                    redirectChain.add(currentUrl)
                }
            } while (response.status.isRedirect() && redirectChain.size < 10)

            val body = if (response.contentType()?.match(ContentType.Text.Html) == true) {
                response.bodyAsText()
            } else null

            val result = LinkResult(
                url = url,
                statusCode = response.status.value,
                responseTimeMs = System.currentTimeMillis() - start,
                redirectChain = redirectChain,
                error = null
            )
            Pair(result, body)
        } catch (e: Exception) {
            val result = LinkResult(
                url = url,
                statusCode = null,
                responseTimeMs = System.currentTimeMillis() - start,
                redirectChain = redirectChain,
                error = e.message ?: "Unknown error"
            )
            Pair(result, null)
        }
    }

    private fun resolveRedirect(from: String, location: String): String {
        return if (location.startsWith("http")) {
            location
        } else {
            val base = java.net.URI(from)
            base.resolve(location).toString()
        }
    }

    private fun HttpStatusCode.isRedirect() = value in 300..399

    override fun close() {
        client.close()
    }
}
