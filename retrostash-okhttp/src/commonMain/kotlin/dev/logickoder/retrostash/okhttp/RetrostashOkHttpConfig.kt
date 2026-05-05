package dev.logickoder.retrostash.okhttp

/**
 * Tunables for [RetrostashOkHttpInterceptor] and `AndroidRetrostashStore`.
 *
 * ## Caching strategy
 *
 * Retrostash maintains its own annotation-driven cache (the [dev.logickoder.retrostash.core.RetrostashStore]).
 * Treat it as the only Retrostash-controlled cache layer. If you also pass `cache(...)` to your
 * `OkHttpClient.Builder`, OkHttp's HTTP disk cache obeys origin `Cache-Control` headers
 * independently — Retrostash invalidation does **not** evict OkHttp HTTP cache entries.
 *
 * Most apps want exactly one cache layer. Drop `cache(...)` from your `OkHttpClient.Builder` and
 * let Retrostash own the lifecycle. See
 * [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy).
 *
 * @property timeoutMs Per-store-call deadline. Slow store I/O falls through to the network.
 * @property maxEntries Soft cap on entries held by the disk store (LRU eviction beyond this).
 * @property maxBytes Soft cap on total persisted bytes (LRU eviction beyond this).
 * @property defaultMaxAgeMs TTL for **Retrostash**'s own store entries when a `@CacheQuery` does
 * not declare one. Applies to every cached payload (GET *and* POST queries).
 * @property logger Optional event hook for cache hits / misses / evictions.
 * @property cacheDirName Subdirectory of `Context.cacheDir` used by `AndroidRetrostashStore`.
 * @property prefsName SharedPreferences name used by `AndroidRetrostashStore` for index storage.
 */
data class RetrostashOkHttpConfig @JvmOverloads constructor(
    val timeoutMs: Long = 250L,
    val maxEntries: Int = 64,
    val maxBytes: Long = 2 * 1024 * 1024L,
    val defaultMaxAgeMs: Long = 10 * 60 * 1000L,
    val logger: ((String) -> Unit)? = null,
    val cacheDirName: String = "retrostash_okhttp_cache",
    val prefsName: String = "retrostash_okhttp_store",
)
