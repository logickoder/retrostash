package dev.logickoder.retrostash.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

data class RetrostashKtorMetadata(
    val scopeName: String,
    val queryTemplate: String? = null,
    val maxAgeMs: Long = 0L,
    val bindings: Map<String, String> = emptyMap(),
    val bodyBytes: ByteArray? = null,
    val invalidateTemplates: List<String> = emptyList(),
)

val RetrostashMetadataKey = AttributeKey<RetrostashKtorMetadata>("retrostash-metadata")

fun HttpRequestBuilder.retrostash(metadata: RetrostashKtorMetadata) {
    attributes.put(RetrostashMetadataKey, metadata)
}
