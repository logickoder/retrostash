package dev.logickoder.retrostash.example.domain

import dev.logickoder.retrostash.example.DemoEngine
import dev.logickoder.retrostash.example.Platform
import dev.logickoder.retrostash.example.model.DemoResult
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import okhttp3.ResponseBody

private const val SOURCE_HEADER = "X-Retrostash-Source"

class RetrofitDemoEngine(
    private val api: RetrofitPostsApi,
    private val client: OkHttpClient,
    private val onLog: (String) -> Unit,
) : DemoEngine {
    override val transport: Transport = Transport.Retrofit

    override suspend fun runQuery(postId: Int): DemoResult = withContext(Dispatchers.Default) {
        val started = Platform.nowMs()
        val response = api.getPost(postId)
        DemoResult(
            transport = transport,
            operation = "GET /posts/$postId",
            statusCode = response.code(),
            source = response.source,
            sizeBytes = response.size,
            durationMs = Platform.nowMs() - started,
        )
    }

    override suspend fun runMutation(postId: Int): DemoResult = withContext(Dispatchers.Default) {
        val started = Platform.nowMs()
        val body = """{"id":$postId,"title":"updated"}"""
            .toRequestBody("application/json".toMediaType())
        val response = api.updatePost(postId, body)
        DemoResult(
            transport = transport,
            operation = "PUT /posts/$postId",
            statusCode = response.code(),
            source = response.source,
            sizeBytes = response.size,
            durationMs = Platform.nowMs() - started,
        )
    }

    override suspend fun clearCache() {
        RetrostashOkHttpBridge.from(client)?.invalidateQueryKey("posts/{id}")
        onLog("Retrofit store cleared")
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
    }


    private val Response<ResponseBody>.source get() = headers()[SOURCE_HEADER] ?: "network"

    private val Response<ResponseBody>.size: Int
        get() {
            val inputStream = body()?.byteStream()
            var totalBytes = 0
            val buffer = ByteArray(8192) // 8KB chunks
            var bytesRead: Int

            inputStream?.use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    // Optionally save the bytes to a file here to keep memory low
                }
            }

            return totalBytes
        }
}
