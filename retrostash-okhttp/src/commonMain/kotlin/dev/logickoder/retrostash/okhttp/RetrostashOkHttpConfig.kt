package dev.logickoder.retrostash.okhttp

/**
 * Tunables for [RetrostashOkHttpInterceptor] and `AndroidRetrostashStore`.
 *
 * ## Caching strategy
 *
 * Retrostash maintains its **own** annotation-driven cache (the [dev.logickoder.retrostash.core.RetrostashStore]).
 * OkHttp ships its **own** HTTP disk cache (`OkHttpClient.Builder().cache(...)`). These are two
 * different caches. Retrostash invalidation (`@CacheMutate`, `bridge.invalidateTag`,
 * `bridge.invalidateQuery`) clears the Retrostash store **only** — it does not evict entries from
 * OkHttp's HTTP cache.
 *
 * If both layers are active and [enableGetCaching] is `true`, GETs end up in **both** caches and a
 * post-invalidation request is served stale from the OkHttp cache (visible as
 * `X-Retrostash-Source: okhttp-cache`).
 *
 * Two correct configurations:
 *  - **Single cache (recommended):** do not pass `cache(...)` to your `OkHttpClient.Builder`.
 *    Retrostash is the only cache. Invalidation is authoritative.
 *  - **Layered:** keep OkHttp's `Cache(...)`, set [enableGetCaching] to `false`, and accept that
 *    Retrostash invalidation does not reach OkHttp's HTTP cache.
 *
 * Full discussion: [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy).
 *
 * @property timeoutMs Per-store-call deadline. Slow store I/O falls through to the network.
 * @property maxEntries Soft cap on entries held by the disk store (LRU eviction beyond this).
 * @property maxBytes Soft cap on total persisted bytes (LRU eviction beyond this).
 * @property defaultMaxAgeMs TTL for **Retrostash**'s own store entries when a `@CacheQuery` does
 * not declare one. Independent of [getMaxAgeSeconds]. Applies to every cached payload (GET *and*
 * POST queries).
 * @property getMaxAgeSeconds **OkHttp HTTP cache** TTL injected as `Cache-Control: public,
 * max-age=<seconds>` on plain `GET` responses when [enableGetCaching] is `true`. This only
 * matters if the consumer also sets `OkHttpClient.Builder().cache(...)`. Orthogonal to
 * [defaultMaxAgeMs] and to `@CacheQuery.maxAgeSeconds`, which control Retrostash's own store TTL.
 * @property enableGetCaching When `true` (the default), the interceptor rewrites outgoing GET
 * responses to `Cache-Control: public, max-age=${getMaxAgeSeconds}` so OkHttp's own HTTP cache
 * holds the body. **Does not** affect Retrostash's store — that path runs unconditionally for
 * `@CacheQuery`. Set to `false` if you want origin `Cache-Control` to win, or if you've removed
 * OkHttp's `Cache(...)` and don't need the rewrite. **Footgun:** keeping this `true` while also
 * setting `OkHttpClient.Builder().cache(...)` creates a stale-data window after Retrostash
 * invalidates — see *Caching strategy* above.
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
