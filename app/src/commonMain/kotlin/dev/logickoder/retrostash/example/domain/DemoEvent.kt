package dev.logickoder.retrostash.example.domain

data class DemoEvent(
    val timestampMs: Long,
    val message: String,
    val isError: Boolean = false,
)