package dev.logickoder.retrostash.model

internal data class Entry(
    val fileName: String,
    val contentType: String?,
    val statusCode: Int,
    val statusMessage: String,
    val headers: List<Pair<String, String>>,
    val size: Long,
    val createdAt: Long,
    val lastAccess: Long,
    val expiresAt: Long
)