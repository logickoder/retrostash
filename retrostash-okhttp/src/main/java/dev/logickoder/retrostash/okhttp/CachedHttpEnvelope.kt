package dev.logickoder.retrostash.okhttp

data class CachedHttpEnvelope(
    val payload: ByteArray,
    val contentType: String?,
    val statusCode: Int,
    val statusMessage: String,
    val headers: List<Pair<String, String>>,
)
