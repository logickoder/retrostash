@file:OptIn(dev.logickoder.retrostash.internal.RetrostashInternalApi::class)

package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.internal.toStringBindings

/**
 * Direct cache control for the Ktor adapter — `peek`, `update`, `invalidate*`, `clearAll`.
 * All methods are `suspend`; call from any coroutine. Mirror surface of [RetrostashOkHttpCache]
 * but uses `scopeName: String` instead of `Class<*>` for KMP compatibility, and stores raw
 * bytes (no envelope — Ktor doesn't synthesize HTTP responses on cache hit, the plugin hands
 * the bytes back via [RetrostashCachedPayloadKey]).
 *
 * Full guidance — including how to source the bytes for `updateQuery` from typed responses
 * and the converter-agnostic philosophy behind that — lives in the README:
 * [Cache API](https://github.com/logickoder/retrostash#cache-api).
 *
 * Obtained via [RetrostashKtorRuntime.cache].
 */
class RetrostashKtorCache internal constructor(
    private val engine: RetrostashEngine,
    private val keyResolver: CoreKeyResolver,
) {

    /**
     * Returns the cached payload bytes for a query, or `null` if no entry, the placeholders
     * couldn't be resolved, or the store call timed out. Decode with whatever serializer you
     * used to write the entry.
     *
     * [bodyBytes] is only needed when [template] contains a placeholder that isn't in
     * [bindings] and must be looked up from a JSON-encoded request body. Most peek calls leave
     * it `null`.
     */
    suspend fun peekQuery(
        scopeName: String,
        template: String,
        bindings: Map<String, Any?>,
        bodyBytes: ByteArray? = null,
    ): ByteArray? {
        val metadata = QueryMetadata(
            scopeName = scopeName,
            template = template,
            bindings = bindings.toStringBindings(),
            bodyBytes = bodyBytes,
        )
        return engine.resolveFromCache(metadata)
    }

    /**
     * Persists [payload] under the resolved cache key for the given query. Returns the
     * resolved cache key, or `null` if any placeholder couldn't be resolved.
     *
     * [tags] are tag templates resolved against the same bindings. The bytes you supply must
     * be in the same shape your API returns — Retrostash doesn't serialize for you. See the
     * README's *Cache API* section for recipes covering kotlinx.serialization, Moshi, and Gson.
     */
    suspend fun updateQuery(
        scopeName: String,
        template: String,
        bindings: Map<String, Any?>,
        payload: ByteArray,
        maxAgeMs: Long = 0L,
        tags: List<String> = emptyList(),
        bodyBytes: ByteArray? = null,
    ): String? {
        val metadata = QueryMetadata(
            scopeName = scopeName,
            template = template,
            bindings = bindings.toStringBindings(),
            bodyBytes = bodyBytes,
            tagTemplates = tags,
        )
        val resolvedKey = keyResolver.resolve(metadata) ?: return null
        engine.persistQueryResult(metadata, payload, maxAgeMs)
        return resolvedKey
    }

    /**
     * Resolves [template] against [bindings] in the namespace of [scopeName] and invalidates
     * the resulting cache key. Returns the resolved key, or `null` if any placeholder couldn't
     * be filled.
     */
    suspend fun invalidateQuery(
        scopeName: String,
        template: String,
        bindings: Map<String, Any?>,
        bodyBytes: ByteArray? = null,
    ): String? {
        val resolvedKey = keyResolver.resolve(
            QueryMetadata(
                scopeName = scopeName,
                template = template,
                bindings = bindings.toStringBindings(),
                bodyBytes = bodyBytes,
            )
        ) ?: return null
        invalidateQueryKey(resolvedKey)
        return resolvedKey
    }

    /** Invalidates a single template/key directly, bypassing annotation extraction. */
    suspend fun invalidateQueryKey(key: String) {
        if (key.isBlank()) return
        engine.invalidateTemplates(listOf(key))
    }

    /**
     * Removes every cached entry whose tag set contains [tag]. The tag must be the **resolved**
     * value (e.g. `"article:concept123"`).
     */
    suspend fun invalidateTag(tag: String) {
        if (tag.isBlank()) return
        engine.invalidateTags(listOf(tag))
    }

    /** Bulk version of [invalidateTag]. Blank values skipped. */
    suspend fun invalidateTags(tags: List<String>) {
        val cleaned = tags.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        engine.invalidateTags(cleaned)
    }

    /** Drops every entry from the underlying store. */
    suspend fun clearAll() {
        engine.clearAll()
    }
}
