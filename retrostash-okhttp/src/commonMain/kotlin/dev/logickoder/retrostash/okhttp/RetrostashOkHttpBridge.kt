package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import kotlinx.coroutines.runBlocking
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
 * @property store Backing cache store.
 * @property config Tunables — TTLs, byte/entry caps, logger.
 * @property keyResolver Custom key resolver (rarely overridden).
 * @property engine Underlying transport-agnostic engine. Exposed for advanced wiring.
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
    /**
     * Adds Retrostash's interceptors to [builder]. Returns [builder] for chaining.
     *
     * Order matters: the handle interceptor must precede the cache interceptor so [from] can
     * recover this bridge from a built [OkHttpClient].
     */
    fun install(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.addInterceptor(RetrostashOkHttpHandleInterceptor(this))
        builder.addInterceptor(RetrostashOkHttpInterceptor(engine, config))
        return builder
    }

    /**
     * Invalidates a single template directly, bypassing annotation extraction. Returns `true` if
     * any work was scheduled (always true for a non-blank key — actual store eviction happens
     * asynchronously inside `runBlocking`).
     */
    fun invalidateQueryKey(key: String): Boolean {
        if (key.isBlank()) return false
        runBlocking {
            engine.invalidateTemplates(listOf(key))
        }
        return true
    }

    /**
     * Resolves [template] against [bindings] in the namespace of [apiClass] and invalidates the
     * resulting cache key. Returns the resolved key (also passed to the store), or `null` if any
     * placeholder could not be filled.
     */
    fun invalidateQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
    ): String? {
        val resolved = keyResolver.resolve(
            QueryMetadata(
                scopeName = apiClass.simpleName,
                template = template,
                bindings = bindings.mapNotNull { (k, v) ->
                    v?.toString()?.let { value -> k to value }
                }.toMap(),
            )
        ) ?: return null

        invalidateQueryKey(resolved)
        return resolved
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
