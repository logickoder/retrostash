package dev.logickoder.retrostash.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

/**
 * Per-request Retrostash metadata attached to a Ktor [HttpRequestBuilder] via [retrostash],
 * [retrostashQuery], or [retrostashMutate]. Read by [RetrostashPlugin] on request and response
 * hooks to drive cache lookup, persistence, and invalidation.
 *
 * @property scopeName Logical namespace (typically the API interface name) — prevents key
 * collisions between unrelated APIs sharing template shapes.
 * @property queryTemplate Cache template (e.g. `"feed/{id}"`). Non-null marks the request as a
 * query candidate for cache lookup and (when [maxAgeMs] > 0) persistence.
 * @property maxAgeMs TTL for persisted entries in milliseconds. `0` disables persistence even
 * when [queryTemplate] is set.
 * @property bindings Pre-extracted placeholder values for [queryTemplate] / [invalidateTemplates].
 * @property bodyBytes Optional raw request body — fallback source for placeholders not in
 * [bindings] (matched via JSON field lookup).
 * @property invalidateTemplates Templates to clear after a successful mutation. Resolved against
 * [bindings] / [bodyBytes] before being passed to the store.
 * @property tagTemplates Tag templates persisted with the cached entry on the query side.
 * Resolved against [bindings] / [bodyBytes] at write time.
 * @property invalidateTagTemplates Tag templates resolved on mutation success and matched
 * against per-entry tag sets in the store.
 */
data class RetrostashKtorMetadata(
    val scopeName: String,
    val queryTemplate: String? = null,
    val maxAgeMs: Long = 0L,
    val bindings: Map<String, String> = emptyMap(),
    val bodyBytes: ByteArray? = null,
    val invalidateTemplates: List<String> = emptyList(),
    val tagTemplates: List<String> = emptyList(),
    val invalidateTagTemplates: List<String> = emptyList(),
)

/** Attribute key used to attach [RetrostashKtorMetadata] to a Ktor request. */
val RetrostashMetadataKey = AttributeKey<RetrostashKtorMetadata>("retrostash-metadata")

/**
 * Attribute key set by [RetrostashPlugin] on cache hits. Read after the request completes to
 * branch on `cache vs network`:
 *
 * ```kotlin
 * val cached = response.call.attributes.getOrNull(RetrostashCachedPayloadKey)
 * val source = if (cached != null) "retrostash-cache" else "network"
 * ```
 */
val RetrostashCachedPayloadKey = AttributeKey<ByteArray>("retrostash-cached-payload")

/**
 * Attaches or merges [metadata] onto this request. Existing metadata fields are preserved when
 * incoming values are blank/null/empty.
 */
fun HttpRequestBuilder.retrostash(metadata: RetrostashKtorMetadata): HttpRequestBuilder {
    val current = attributes.getOrNull(RetrostashMetadataKey)
    attributes.put(RetrostashMetadataKey, mergeRetrostashMetadata(current, metadata))
    return this
}

/**
 * Marks this request as a Retrostash query. On hit, the plugin returns the cached payload via
 * [RetrostashCachedPayloadKey]. On miss, the response is persisted (if [maxAgeMs] > 0) on a 2xx
 * status code.
 *
 * @param scopeName Logical namespace (typically the API interface name).
 * @param template Cache template (e.g. `"feed/{id}"`).
 * @param bindings Placeholder values from the call site (path / query parameters).
 * @param bodyBytes Optional request body for placeholder fallback resolution.
 * @param maxAgeMs TTL for the cached entry. `0` disables persistence (lookup-only).
 * @param tags Tag templates to persist with the entry. Resolved from [bindings] / [bodyBytes].
 */
fun HttpRequestBuilder.retrostashQuery(
    scopeName: String,
    template: String,
    bindings: Map<String, String> = emptyMap(),
    bodyBytes: ByteArray? = null,
    maxAgeMs: Long = 0L,
    tags: List<String> = emptyList(),
): HttpRequestBuilder {
    return retrostash(
        RetrostashKtorMetadata(
            scopeName = scopeName,
            queryTemplate = template,
            maxAgeMs = maxAgeMs,
            bindings = bindings,
            bodyBytes = bodyBytes,
            tagTemplates = tags,
        )
    )
}

/**
 * Marks this request as a Retrostash mutation. On a 2xx response, [RetrostashPlugin] resolves
 * each entry in [invalidateTemplates] against [bindings] / [bodyBytes] and removes matching
 * cache entries.
 *
 * @param scopeName Logical namespace; should match the [scopeName] used by paired query calls.
 * @param invalidateTemplates Cache templates to clear. Use the placeholder form
 * (e.g. `"users/{id}"`) — the plugin substitutes from [bindings] before invalidating.
 * @param bindings Placeholder values from the call site.
 * @param bodyBytes Optional request body for placeholder fallback resolution.
 * @param invalidateTags Tag templates to resolve and clear on a 2xx response (parallel to
 * [invalidateTemplates] but matched against per-entry tag sets).
 */
fun HttpRequestBuilder.retrostashMutate(
    scopeName: String,
    invalidateTemplates: List<String> = emptyList(),
    bindings: Map<String, String> = emptyMap(),
    bodyBytes: ByteArray? = null,
    invalidateTags: List<String> = emptyList(),
): HttpRequestBuilder {
    return retrostash(
        RetrostashKtorMetadata(
            scopeName = scopeName,
            bindings = bindings,
            bodyBytes = bodyBytes,
            invalidateTemplates = invalidateTemplates,
            invalidateTagTemplates = invalidateTags,
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
    val tagTemplates = (current.tagTemplates + incoming.tagTemplates).distinct()
    val invalidateTagTemplates =
        (current.invalidateTagTemplates + incoming.invalidateTagTemplates).distinct()

    return RetrostashKtorMetadata(
        scopeName = scope,
        queryTemplate = queryTemplate,
        maxAgeMs = maxAgeMs,
        bindings = bindings,
        bodyBytes = bodyBytes,
        invalidateTemplates = invalidations,
        tagTemplates = tagTemplates,
        invalidateTagTemplates = invalidateTagTemplates,
    )
}
