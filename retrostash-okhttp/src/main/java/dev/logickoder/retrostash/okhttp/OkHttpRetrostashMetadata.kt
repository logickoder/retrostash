package dev.logickoder.retrostash.okhttp

data class OkHttpRetrostashMetadata(
    val scopeName: String,
    val queryTemplate: String? = null,
    val maxAgeMs: Long = 0L,
    val bindings: Map<String, String> = emptyMap(),
    val invalidateTemplates: List<String> = emptyList(),
)
