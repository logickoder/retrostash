package dev.logickoder.retrostash.core

/**
 * Transport-agnostic core of Retrostash. Owns the [RetrostashStore] and resolves cache keys via
 * [CoreKeyResolver]. Each store call is wrapped in a `withTimeoutOrNull` of [timeoutMs] so a
 * stuck or slow store can't block a request — the call falls through to the network instead.
 *
 * Adapters (`RetrostashOkHttpInterceptor`, `RetrostashKtorRuntime`) construct one engine per
 * client and dispatch transport metadata into [resolveFromCache], [persistQueryResult],
 * [invalidateTemplates], [invalidateTags], and [clearAll].
 *
 * @property store Backing store for cached payloads.
 * @property keyResolver Builds the persistence key from [QueryMetadata]. Default uses the
 * standard `scopeName|<resolvedTemplate>|<hash>` shape.
 * @property timeoutMs Per-store-call deadline in milliseconds. Hits `null`/no-op on expiry.
 */
class RetrostashEngine(
    private val store: RetrostashStore,
    private val keyResolver: CoreKeyResolver = CoreKeyResolver(),
    private val timeoutMs: Long = 250L,
) {

    /**
     * Returns the cached payload for [metadata], or `null` if the key cannot be resolved (missing
     * placeholder bindings), the store has no entry, or the store call exceeded [timeoutMs].
     */
    suspend fun resolveFromCache(metadata: QueryMetadata): ByteArray? {
        val key = keyResolver.resolve(metadata) ?: return null
        return withStoreTimeoutOrNull(timeoutMs) {
            store.get(key)
        }
    }

    /**
     * Persists [payload] for the resolved [metadata] key with TTL [maxAgeMs]. Tag templates on
     * [metadata] are resolved against the same bindings / body and persisted alongside the entry.
     * No-op if the key cannot be resolved or the store call times out.
     */
    suspend fun persistQueryResult(metadata: QueryMetadata, payload: ByteArray, maxAgeMs: Long) {
        val key = keyResolver.resolve(metadata) ?: return
        val tags = keyResolver.resolveTags(metadata).toSet()
        withStoreTimeoutOrNull(timeoutMs) {
            store.put(key, payload, maxAgeMs, tags)
        }
    }

    /**
     * PATCH-style write. Replaces [payload] under the resolved [metadata] key. [maxAgeMs] and
     * [tagsOverride] use null-means-preserve semantics (see [RetrostashStore.patch]).
     *
     * The cache layer is responsible for deciding when to pass `null` (preserve) vs a value
     * (override); this method just forwards to the store.
     */
    suspend fun patchQueryResult(
        metadata: QueryMetadata,
        payload: ByteArray,
        maxAgeMs: Long? = null,
        tagsOverride: Set<String>? = null,
    ) {
        val key = keyResolver.resolve(metadata) ?: return
        withStoreTimeoutOrNull(timeoutMs) {
            store.patch(key, payload, maxAgeMs, tagsOverride)
        }
    }

    /**
     * Invalidates every entry whose key contains `|<template>|` for any of [templates].
     *
     * Templates must already be **resolved** (placeholders substituted with the mutation's actual
     * bindings). The transport layer is responsible for substitution before calling this — see
     * `RetrostashOkHttpInterceptor` and `RetrostashKtorRuntime`.
     */
    suspend fun invalidateTemplates(templates: List<String>) {
        templates
            .filter { it.isNotBlank() }
            .forEach { template ->
                withStoreTimeoutOrNull(timeoutMs) {
                    store.invalidate(template)
                }
            }
    }

    /**
     * Invalidates every entry whose tag set contains any of [tags]. [tags] must be the
     * **resolved** values (e.g. `"article:concept123"`, not `"article:{conceptId}"`).
     */
    suspend fun invalidateTags(tags: List<String>) {
        tags
            .filter { it.isNotBlank() }
            .forEach { tag ->
                withStoreTimeoutOrNull(timeoutMs) {
                    store.invalidateTag(tag)
                }
            }
    }

    /** Drops every entry from the underlying store. */
    suspend fun clearAll() {
        withStoreTimeoutOrNull(timeoutMs) {
            store.clear()
        }
    }
}
