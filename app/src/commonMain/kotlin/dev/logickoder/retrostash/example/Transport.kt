package dev.logickoder.retrostash.example

enum class Transport(val label: String) {
    Ktor("Ktor"),
    OkHttp("OkHttp"),
}

data class DemoEvent(
    val timestampMs: Long,
    val message: String,
    val isError: Boolean = false,
)

data class DemoResult(
    val transport: Transport,
    val operation: String,
    val statusCode: Int,
    val source: String,
    val sizeBytes: Int,
    val durationMs: Long,
)

interface DemoEngine {
    val transport: Transport
    suspend fun runQuery(postId: Int): DemoResult
    suspend fun runMutation(postId: Int): DemoResult
    suspend fun clearCache()
    fun close()
}
