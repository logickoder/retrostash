package dev.logickoder.retrostash.model

internal data class Entry(
    val fileName: String,
    val contentType: String?,
    val size: Long,
    val createdAt: Long,
    val lastAccess: Long,
    val expiresAt: Long
)