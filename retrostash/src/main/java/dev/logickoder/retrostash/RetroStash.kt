package dev.logickoder.retrostash

import android.content.Context
import dev.logickoder.retrostash.interceptor.CacheInterceptor
import dev.logickoder.retrostash.interceptor.NetworkCachePolicyInterceptor
import okhttp3.OkHttpClient

/**
 * Entry point for integrating Retrostash with an OkHttp plus Retrofit stack.
 */
object Retrostash {

    /**
     * Creates all runtime components without mutating an OkHttp builder.
     *
     * @param context application context used for persistent cache/invalidation storage.
     * @param config Retrostash runtime configuration.
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        context: Context,
        config: RetrostashConfig = RetrostashConfig(),
    ): RetrostashRuntime {
        val keyResolver = NetworkCacheKeyResolver()
        val invalidator = NetworkCacheInvalidator(context, ttlMs = config.invalidationTtlMs)
        val postCacheStore = PostResponseCacheStore(
            context = context,
            maxEntries = config.postCacheMaxEntries,
            maxBytes = config.postCacheMaxBytes,
            ttlMs = config.postCacheTtlMs,
        )
        val cacheControlInterceptor = CacheInterceptor(
            maxAgeSeconds = config.getMaxAgeSeconds,
            enableGetCaching = config.enableGetCaching,
        )
        val networkPolicyInterceptor = NetworkCachePolicyInterceptor(
            keyResolver = keyResolver,
            invalidator = invalidator,
            postCacheStore = postCacheStore,
        )
        return RetrostashRuntime(
            keyResolver = keyResolver,
            invalidator = invalidator,
            postCacheStore = postCacheStore,
            cacheControlInterceptor = cacheControlInterceptor,
            networkPolicyInterceptor = networkPolicyInterceptor,
        )
    }

    /**
     * Installs Retrostash interceptors on [builder] and returns created runtime components.
     *
     * @param builder target OkHttp builder.
     * @param context application context used for persistent cache/invalidation storage.
     * @param config Retrostash runtime configuration.
     */
    @JvmStatic
    @JvmOverloads
    fun install(
        builder: OkHttpClient.Builder,
        context: Context,
        config: RetrostashConfig = RetrostashConfig(),
    ): RetrostashRuntime {
        val runtime = create(context, config)
        builder.addInterceptor(runtime.networkPolicyInterceptor)
        builder.addNetworkInterceptor(runtime.cacheControlInterceptor)
        return runtime
    }

    /**
     * Clears all persisted Retrostash state.
     *
     * @param context application context used for persistent cache/invalidation storage.
     */
    @JvmStatic
    fun clear(context: Context) {
        NetworkCacheInvalidator.clear(context)
        PostResponseCacheStore.clearStorage(context)
    }
}
