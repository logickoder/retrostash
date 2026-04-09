package dev.logickoder.retrostash

/**
 * Configuration used by Retrostash when creating runtime components.
 *
 * @property getMaxAgeSeconds max-age value used when rewriting GET cache headers.
 * @property enableGetCaching enables GET cache header rewriting.
 * @property invalidationTtlMs TTL for mutation invalidation markers.
 * @property postCacheMaxEntries maximum number of persisted POST cache entries.
 * @property postCacheMaxBytes maximum total bytes for persisted POST cache entries.
 * @property postCacheTtlMs TTL for persisted POST cache entries.
 */
data class RetrostashConfig @JvmOverloads constructor(
    val getMaxAgeSeconds: Long = 24 * 60 * 60L,
    val enableGetCaching: Boolean = true,
    val invalidationTtlMs: Long = NetworkCacheInvalidator.DEFAULT_TTL_MS,
    val postCacheMaxEntries: Int = 32,
    val postCacheMaxBytes: Long = 2 * 1024 * 1024L,
    val postCacheTtlMs: Long = 10 * 60 * 1000L,
)
