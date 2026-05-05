package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import okhttp3.OkHttpClient

/**
 * Top-level OkHttp/Retrofit integration object. Holds the [RetrostashEngine] + [store] and
 * installs the two interceptors needed to read/write the cache:
 *  - `RetrostashOkHttpHandleInterceptor` (application interceptor) — exposes the bridge so
 *    callers can recover it via [from].
 *  - [RetrostashOkHttpInterceptor] (network interceptor) — actual cache logic.
 *
 * For Android, prefer the convenience factory `RetrostashOkHttpAndroid.install(builder, context)`
 * which constructs a Context-backed disk store automatically.
 *
 * Pure-JVM consumers construct the bridge directly with their own [RetrostashStore]:
 *
 * ```kotlin
 * val bridge = RetrostashOkHttpBridge(
 *     store = InMemoryRetrostashStore(),
 *     config = RetrostashOkHttpConfig(logger = ::println),
 * )
 * val client = OkHttpClient.Builder().also(bridge::install).build()
 * ```
 *
 * Direct cache control — peek, update, invalidate, clear — lives on [cache]. See
 * [RetrostashOkHttpCache] and the README's
 * [Cache API](https://github.com/logickoder/retrostash#cache-api) section.
 *
 * @property store Backing cache store.
 * @property config Tunables — TTLs, byte/entry caps, logger.
 * @property keyResolver Custom key resolver (rarely overridden).
 * @property engine Underlying transport-agnostic engine. Exposed for advanced wiring.
 * @property cache Direct cache surface — `peekQuery`, `updateQuery`, `invalidateQuery*`,
 * `invalidateTag(s)`, `clearAll`.
 */
class RetrostashOkHttpBridge(
    val store: RetrostashStore,
    private val config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
    private val keyResolver: CoreKeyResolver = CoreKeyResolver(),
    val engine: RetrostashEngine = RetrostashEngine(
        store = store,
        keyResolver = keyResolver,
        timeoutMs = config.timeoutMs,
    ),
) {
    val cache: RetrostashOkHttpCache = RetrostashOkHttpCache(engine, keyResolver, store)

    /**
     * Adds Retrostash's interceptors to [builder]. Returns [builder] for chaining.
     *
     * Order matters: the handle interceptor must precede the cache interceptor so [from] can
     * recover this bridge from a built [OkHttpClient].
     *
     * **Caching strategy:** if [builder] also has `cache(...)` set, you have two cache layers
     * (Retrostash store + OkHttp HTTP cache). Retrostash invalidation does **not** evict OkHttp
     * HTTP cache entries — see [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy)
     * for the recommended configuration.
     */
    fun install(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.addInterceptor(RetrostashOkHttpHandleInterceptor(this))
        builder.addInterceptor(RetrostashOkHttpInterceptor(engine, config))
        return builder
    }

    companion object {
        /**
         * Recovers the bridge previously installed onto [client], or `null` if the client was
         * built without Retrostash. Useful when external code (e.g. a `Logout` flow) needs to
         * invalidate cache entries without re-plumbing the bridge through DI.
         */
        fun from(client: OkHttpClient): RetrostashOkHttpBridge? {
            return client.interceptors
                .asSequence()
                .filterIsInstance<RetrostashOkHttpHandleInterceptor>()
                .map { it.bridge }
                .firstOrNull()
        }
    }
}
