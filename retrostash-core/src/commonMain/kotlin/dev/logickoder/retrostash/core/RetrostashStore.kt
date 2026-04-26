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
     */
    suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long)

    /**
     * Removes any entries whose key contains `|`[template]`|` as a substring, plus the literal
     * key match if present. [template] should be the resolved (placeholder-substituted) form
     * (e.g. `users/42`, not `users/{id}`). Callers are responsible for the substitution.
     */
    suspend fun invalidate(template: String)

    /** Drops every entry. Used by external "clear cache" actions. */
    suspend fun clear()
}
