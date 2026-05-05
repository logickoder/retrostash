package dev.logickoder.retrostash.core

/**
 * Persistent or in-memory backing store for cached query payloads.
 *
 * Implementations are responsible for thread-safe concurrent access, expiry, and any capacity
 * eviction. Keys are produced by [CoreKeyResolver] and have the shape
 * `scopeName|<resolvedTemplate>|<binding-hash>`. Templates passed to [invalidate] are matched
 * against the middle segment by substring.
 *
 * Built-in implementations:
 *  - [InMemoryRetrostashStore] — LinkedHashMap + Mutex, KMP-safe, ephemeral.
 *  - `AndroidRetrostashStore` (in `retrostash-okhttp`) — Android Context-backed disk store.
 */
interface RetrostashStore {

    /**
     * Returns the cached payload for [key], or `null` if not present, expired, or unrecoverable.
     * Implementations may evict expired entries as a side effect.
     */
    suspend fun get(key: String): ByteArray?

    /**
     * Stores [payload] under [key] with optional [maxAgeMs] TTL. `0` or negative means no
     * explicit expiry — the entry lives until invalidated, cleared, or evicted.
     *
     * [tags] are persisted alongside the entry and used by [invalidateTag] to clear groups of
     * entries that share a logical identifier (typically resolved from `@CacheQuery.tags`).
     */
    suspend fun put(
        key: String,
        payload: ByteArray,
        maxAgeMs: Long,
        tags: Set<String> = emptySet(),
    )

    /**
     * PATCH-style write. Replaces the [payload] of [key] but **preserves** [maxAgeMs] and
     * [tags] when their argument is `null`.
     *
     * Semantics per parameter:
     *  - `maxAgeMs = null` → keep existing entry's TTL. `0` → no explicit expiry. `>0` → use it.
     *  - `tags = null` → keep existing entry's tags. `emptySet()` → explicitly clear.
     *    Non-empty → replace.
     *
     * The `createdAt` timestamp resets — a patch is a new write, freshness window restarts.
     * If [key] has no existing entry, falls back to: `maxAgeMs = 0`, `tags = emptySet()`.
     */
    suspend fun patch(
        key: String,
        payload: ByteArray,
        maxAgeMs: Long? = null,
        tags: Set<String>? = null,
    )

    /**
     * Removes any entries whose key contains `|`[template]`|` as a substring, plus the literal
     * key match if present. [template] should be the resolved (placeholder-substituted) form
     * (e.g. `users/42`, not `users/{id}`). Callers are responsible for the substitution.
     */
    suspend fun invalidate(template: String)

    /**
     * Removes every entry whose tag set contains [tag]. [tag] must be the **resolved** tag value
     * (e.g. `"article:concept123"`). No-op for blank input or when no entries carry that tag.
     */
    suspend fun invalidateTag(tag: String)

    /** Drops every entry. Used by external "clear cache" actions. */
    suspend fun clear()
}
