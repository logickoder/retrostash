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
val RetrostashCachedPayloadKey = AttributeKey<ByteArray>("retrostash-cached-payload")

fun HttpRequestBuilder.retrostash(metadata: RetrostashKtorMetadata): HttpRequestBuilder {
    val current = attributes.getOrNull(RetrostashMetadataKey)
    attributes.put(RetrostashMetadataKey, mergeRetrostashMetadata(current, metadata))
    return this
}

fun HttpRequestBuilder.retrostashQuery(
    scopeName: String,
    template: String,
    bindings: Map<String, String> = emptyMap(),
    bodyBytes: ByteArray? = null,
    maxAgeMs: Long = 0L,
): HttpRequestBuilder {
    return retrostash(
        RetrostashKtorMetadata(
            scopeName = scopeName,
            queryTemplate = template,
            maxAgeMs = maxAgeMs,
            bindings = bindings,
            bodyBytes = bodyBytes,
        )
    )
}

fun HttpRequestBuilder.retrostashMutate(
    scopeName: String,
    invalidateTemplates: List<String>,
    bindings: Map<String, String> = emptyMap(),
    bodyBytes: ByteArray? = null,
): HttpRequestBuilder {
    return retrostash(
        RetrostashKtorMetadata(
            scopeName = scopeName,
            bindings = bindings,
            bodyBytes = bodyBytes,
            invalidateTemplates = invalidateTemplates,
        )
    )
}

private fun mergeRetrostashMetadata(
    current: RetrostashKtorMetadata?,
    incoming: RetrostashKtorMetadata,
): RetrostashKtorMetadata {
    if (current == null) return incoming

    val scope = if (incoming.scopeName.isBlank()) current.scopeName else incoming.scopeName
    val queryTemplate = incoming.queryTemplate ?: current.queryTemplate
    val maxAgeMs = if (incoming.maxAgeMs > 0L) incoming.maxAgeMs else current.maxAgeMs
    val bindings = if (incoming.bindings.isNotEmpty()) {
        current.bindings + incoming.bindings
    } else {
        current.bindings
    }
    val bodyBytes = incoming.bodyBytes ?: current.bodyBytes
    val invalidations = (current.invalidateTemplates + incoming.invalidateTemplates).distinct()

    return RetrostashKtorMetadata(
        scopeName = scope,
        queryTemplate = queryTemplate,
        maxAgeMs = maxAgeMs,
        bindings = bindings,
        bodyBytes = bodyBytes,
        invalidateTemplates = invalidations,
    )
}
