package dev.logickoder.retrostash.okhttp

/**
 * Tunables for [RetrostashOkHttpInterceptor] and `AndroidRetrostashStore`.
 *
 * @property timeoutMs Per-store-call deadline. Slow store I/O falls through to the network.
 * @property maxEntries Soft cap on entries held by the disk store (LRU eviction beyond this).
 * @property maxBytes Soft cap on total persisted bytes (LRU eviction beyond this).
 * @property defaultMaxAgeMs TTL applied to entries when a `@CacheQuery` does not declare one.
 * @property getMaxAgeSeconds Cache-Control max-age (seconds) injected for plain `GET` responses
 * when [enableGetCaching] is `true` — lets OkHttp's own HTTP cache hold the body.
 * @property enableGetCaching Whether to rewrite `Cache-Control` on outgoing GETs.
 * @property logger Optional event hook for cache hits / misses / evictions.
 * @property cacheDirName Subdirectory of `Context.cacheDir` used by `AndroidRetrostashStore`.
 * @property prefsName SharedPreferences name used by `AndroidRetrostashStore` for index storage.
 */
data class RetrostashOkHttpConfig @JvmOverloads constructor(
    val timeoutMs: Long = 250L,
    val maxEntries: Int = 64,
    val maxBytes: Long = 2 * 1024 * 1024L,
    val defaultMaxAgeMs: Long = 10 * 60 * 1000L,
    val getMaxAgeSeconds: Long = 24 * 60 * 60L,
    val enableGetCaching: Boolean = true,
    val logger: ((String) -> Unit)? = null,
    val cacheDirName: String = "retrostash_okhttp_cache",
    val prefsName: String = "retrostash_okhttp_store",
)
