package dev.logickoder.retrostash.example.domain

import dev.logickoder.retrostash.example.DemoEngine
import dev.logickoder.retrostash.example.Platform
import dev.logickoder.retrostash.example.nowMs
import dev.logickoder.retrostash.example.model.DemoResult
import dev.logickoder.retrostash.okhttp.OkHttpRetrostashMetadata
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import dev.logickoder.retrostash.okhttp.retrostash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val BASE_URL = "https://jsonplaceholder.typicode.com"
private const val SOURCE_HEADER = "X-Retrostash-Source"

class OkHttpDemoEngine(
    private val client: OkHttpClient,
    private val onLog: (String) -> Unit,
) : DemoEngine {
    override val transport: Transport = Transport.OkHttp

    override suspend fun runQuery(postId: Int): DemoResult = withContext(Dispatchers.Default) {
        val started = Platform.nowMs()
        val request = Request.Builder()
            .url("$BASE_URL/posts/$postId")
            .get()
            .retrostash(
                OkHttpRetrostashMetadata(
                    scopeName = "PostsApi",
                    queryTemplate = "posts/{id}",
                    bindings = mapOf("id" to postId.toString()),
                    maxAgeMs = 60_000L,
                )
            )
            .build()

        client.newCall(request).execute().use { response ->
            val bytes = response.peekBody(Long.MAX_VALUE).bytes()
            val source = response.header(SOURCE_HEADER) ?: "network"
            DemoResult(
                transport = transport,
                operation = "GET /posts/$postId",
                statusCode = response.code,
                source = source,
                sizeBytes = bytes.size,
                durationMs = Platform.nowMs() - started,
            )
        }
    }

    override suspend fun runMutation(postId: Int): DemoResult = withContext(Dispatchers.Default) {
        val started = Platform.nowMs()
        val request = Request.Builder()
            .url("$BASE_URL/posts/$postId")
            .put(
                """{"id":$postId,"title":"updated"}"""
                    .toRequestBody("application/json".toMediaType())
            )
            .retrostash(
                OkHttpRetrostashMetadata(
                    scopeName = "PostsApi",
                    invalidateTemplates = listOf("posts/{id}"),
                    bindings = mapOf("id" to postId.toString()),
                )
            )
            .build()

        client.newCall(request).execute().use { response ->
            val bytes = response.peekBody(Long.MAX_VALUE).bytes()
            val source = response.header(SOURCE_HEADER) ?: "network"
            DemoResult(
                transport = transport,
                operation = "PUT /posts/$postId",
                statusCode = response.code,
                source = source,
                sizeBytes = bytes.size,
                durationMs = Platform.nowMs() - started,
            )
        }
    }

    override suspend fun clearCache() {
        val bridge = RetrostashOkHttpBridge.from(client)
        bridge?.invalidateQueryKey("posts/{id}")
        onLog("OkHttp store cleared")
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
    }
}
