package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.ktor.RetrostashCachedPayloadKey
import dev.logickoder.retrostash.ktor.RetrostashPlugin
import dev.logickoder.retrostash.ktor.retrostashMutate
import dev.logickoder.retrostash.ktor.retrostashQuery
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val BASE_URL = "https://jsonplaceholder.typicode.com"

class KtorDemoEngine(
    private val store: RetrostashStore,
    private val onLog: (String) -> Unit,
) : DemoEngine {
    override val transport: Transport = Transport.Ktor

    private val client: HttpClient = HttpClient(createPlatformKtorEngine()) {
        install(RetrostashPlugin) {
            this.store = this@KtorDemoEngine.store
            this.timeoutMs = 1_000L
            this.logger = onLog
        }
    }

    override suspend fun runQuery(postId: Int): DemoResult {
        val started = nowMs()
        val response: HttpResponse = client.get("$BASE_URL/posts/$postId") {
            retrostashQuery(
                scopeName = "PostsApi",
                template = "posts/{id}",
                bindings = mapOf("id" to postId.toString()),
                maxAgeMs = 60_000L,
            )
        }
        val cached = response.call.attributes.getOrNull(RetrostashCachedPayloadKey)
        val bytes = cached ?: response.bodyAsBytes()
        val source = if (cached != null) "retrostash-cache" else "network"
        return DemoResult(
            transport = transport,
            operation = "GET /posts/$postId",
            statusCode = response.status.value,
            source = source,
            sizeBytes = bytes.size,
            durationMs = nowMs() - started,
        )
    }

    override suspend fun runMutation(postId: Int): DemoResult {
        val started = nowMs()
        val response: HttpResponse = client.post("$BASE_URL/posts/$postId") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":$postId,"title":"updated"}""")
            retrostashMutate(
                scopeName = "PostsApi",
                invalidateTemplates = listOf("posts/{id}"),
                bindings = mapOf("id" to postId.toString()),
            )
        }
        val bytes = response.bodyAsBytes()
        return DemoResult(
            transport = transport,
            operation = "POST /posts/$postId",
            statusCode = response.status.value,
            source = "network",
            sizeBytes = bytes.size,
            durationMs = nowMs() - started,
        )
    }

    override suspend fun clearCache() {
        store.clear()
    }

    override fun close() {
        client.close()
    }
}
