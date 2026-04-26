package dev.logickoder.retrostash.example.model

import dev.logickoder.retrostash.example.domain.Transport

data class DemoResult(
    val transport: Transport,
    val operation: String,
    val statusCode: Int,
    val source: String,
    val sizeBytes: Int,
    val durationMs: Long,
)