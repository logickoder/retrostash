@file:OptIn(dev.logickoder.retrostash.internal.RetrostashInternalApi::class)

package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.internal.toStringBindings
import kotlinx.coroutines.runBlocking

/**
 * Direct cache control for the OkHttp / Retrofit adapter — `peek`, `update`, `invalidate*`,
 * `clearAll`. Methods are blocking; each call wraps a single coroutine via `runBlocking` so
 * Android consumers don't have to manage a coroutine scope.
 *
 * Entries are wrapped in a synthetic [CachedHttpEnvelope] on `updateQuery` (status 200, the
 * supplied content-type, no headers) so they're indistinguishable from interceptor-written
 * entries on read-back.
 *
 * Full guidance — including how to source the bytes for `updateQuery` from
 * `Response<MyDto>` / `Response<String>` and the converter-agnostic philosophy behind that —
 * lives in the README:
 * [Cache API](https://github.com/logickoder/retrostash#cache-api).
 *
 * Obtained via [RetrostashOkHttpBridge.cache].
 */
class RetrostashOkHttpCache internal constructor(
    private val engine: RetrostashEngine,
    private val keyResolver: CoreKeyResolver,
    private val store: RetrostashStore,
) {

    /**
     * Returns the cached payload for a query, or `null` if no entry, the placeholders couldn't
     * be resolved, or the store call timed out. The returned bytes are envelope-unwrapped — the
     * same body bytes the network would have returned.
     *
     * Decode with whatever serializer you used to write the entry (the interceptor uses the
     * raw network bytes). Example with kotlinx.serialization:
     *
     * ```kotlin
     * val raw = bridge.cache.peekQuery(UserApi::class.java, "users/{id}", mapOf("id" to "42"))
     *     ?: return  // not cached
     * val user: UserDto = Json.decodeFromString(raw.decodeToString())
     * ```
     *
     * [bodyBytes] is only needed when [template] contains a placeholder that's not in
     * [bindings] and must be resolved from a JSON-encoded request body. Most peek calls leave
     * it `null`.
     */
    fun peekQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
        bodyBytes: ByteArray? = null,
    ): ByteArray? {
        val resolvedKey = resolveKey(apiClass, template, bindings, bodyBytes) ?: return null
        val raw = runBlocking { store.get(resolvedKey) } ?: return null
        return CachedHttpEnvelopeCodec.decode(raw)?.payload ?: raw
    }

    /**
     * PATCH-style write. Persists [payload] under the resolved cache key for the given query,
     * wrapped in a synthetic 200 OK envelope. Returns the resolved cache key, or `null` if any
     * placeholder in [template] couldn't be resolved.
     *
     * Each metadata parameter has null-means-preserve semantics:
     *  - [contentType] = `null` → keep the existing entry's `Content-Type` (or
     *    `"application/json"` if no entry exists yet).
     *  - [maxAgeMs] = `null` → keep the existing entry's TTL. `0L` → no explicit expiry. `>0`
     *    → use it.
     *  - [tags] = `null` → keep the existing entry's tags. `emptyList()` → explicitly clear.
     *    Non-empty → resolve as tag templates against [bindings] / [bodyBytes] and replace.
     *
     * The freshness window restarts on every patch (a new write resets `createdAt`).
     *
     * Common recipes for the byte payload:
     *
     * ```kotlin
     * // Optimistic update with kotlinx.serialization — preserve tags from the original
     * // @CacheQuery write
     * val payload = Json.encodeToString(updatedUser).encodeToByteArray()
     * bridge.cache.updateQuery(
     *     UserApi::class.java,
     *     template = "users/{id}",
     *     bindings = mapOf("id" to "42"),
     *     payload = payload,
     *     // contentType, maxAgeMs, tags omitted → all preserved
     * )
     *
     * // Explicitly replace tags
     * bridge.cache.updateQuery(
     *     ...,
     *     payload = payload,
     *     tags = listOf("user:{id}", "tenant:{tenantId}"),
     * )
     *
     * // From a Retrofit Response<ResponseBody>
     * val payload = response.body()?.bytes() ?: return
     * ```
     *
     * Footgun: if the bytes you write here drift from what the server would return, every read
     * after this `updateQuery` lies until the next mutation/invalidation refreshes the entry.
     */
    fun updateQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
        payload: ByteArray,
        contentType: String? = null,
        maxAgeMs: Long? = null,
        tags: List<String>? = null,
        bodyBytes: ByteArray? = null,
    ): String? {
        val tagTemplates = tags ?: emptyList()
        val metadata = QueryMetadata(
            scopeName = apiClass.simpleName,
            template = template,
            bindings = bindings.toStringBindings(),
            bodyBytes = bodyBytes,
            tagTemplates = tagTemplates,
        )
        val resolvedKey = keyResolver.resolve(metadata) ?: return null

        val resolvedContentType = contentType
            ?: existingEnvelope(resolvedKey)?.contentType
            ?: "application/json"
        val envelope = CachedHttpEnvelope(
            payload = payload,
            contentType = resolvedContentType,
            statusCode = 200,
            statusMessage = "OK",
            headers = emptyList(),
        )

        val tagsOverride: Set<String>? = tags?.let { keyResolver.resolveTags(metadata).toSet() }

        runBlocking {
            engine.patchQueryResult(
                metadata = metadata,
                payload = CachedHttpEnvelopeCodec.encode(envelope),
                maxAgeMs = maxAgeMs,
                tagsOverride = tagsOverride,
            )
        }
        return resolvedKey
    }

    private fun existingEnvelope(key: String): CachedHttpEnvelope? {
        val raw = runBlocking { store.get(key) } ?: return null
        return CachedHttpEnvelopeCodec.decode(raw)
    }

    /**
     * Resolves [template] against [bindings] in the namespace of [apiClass] and invalidates the
     * resulting cache key. Returns the resolved key, or `null` if any placeholder could not be
     * filled.
     */
    fun invalidateQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
        bodyBytes: ByteArray? = null,
    ): String? {
        val resolvedKey = resolveKey(apiClass, template, bindings, bodyBytes) ?: return null
        invalidateQueryKey(resolvedKey)
        return resolvedKey
    }

    /**
     * Invalidates a single template/key directly, bypassing annotation extraction. Returns
     * `true` if a non-blank key was scheduled for invalidation.
     */
    fun invalidateQueryKey(key: String): Boolean {
        if (key.isBlank()) return false
        runBlocking {
            engine.invalidateTemplates(listOf(key))
        }
        return true
    }

    /**
     * Removes every cached entry whose tag set contains [tag]. The tag must be the **resolved**
     * value (e.g. `"article:concept123"`). Returns `true` if a non-blank tag was scheduled.
     */
    fun invalidateTag(tag: String): Boolean {
        if (tag.isBlank()) return false
        runBlocking {
            engine.invalidateTags(listOf(tag))
        }
        return true
    }

    /** Bulk version of [invalidateTag]. Blank values are skipped. */
    fun invalidateTags(vararg tags: String): Boolean {
        val cleaned = tags.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return false
        runBlocking {
            engine.invalidateTags(cleaned)
        }
        return true
    }

    /** Drops every entry from the underlying store. */
    fun clearAll() {
        runBlocking {
            engine.clearAll()
        }
    }

    private fun resolveKey(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
        bodyBytes: ByteArray?,
    ): String? = keyResolver.resolve(
        QueryMetadata(
            scopeName = apiClass.simpleName,
            template = template,
            bindings = bindings.toStringBindings(),
            bodyBytes = bodyBytes,
        )
    )
}
