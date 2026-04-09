package dev.logickoder.retrostash

import android.content.Context
import dev.logickoder.retrostash.interceptor.CacheControlInterceptor
import dev.logickoder.retrostash.interceptor.NetworkCachePolicyInterceptor
import dev.logickoder.retrostash.interceptor.ResponseSourceInterceptor
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
        val keyResolver = NetworkCacheKeyResolver(logger = config.logger)
        val invalidator = NetworkCacheInvalidator(context, ttlMs = config.invalidationTtlMs)
        val postCacheStore = PostResponseCacheStore(
            context = context,
            maxEntries = config.postCacheMaxEntries,
            maxBytes = config.postCacheMaxBytes,
            ttlMs = config.postCacheTtlMs,
        )
        val cacheControlInterceptor = CacheControlInterceptor(
            maxAgeSeconds = config.getMaxAgeSeconds,
            enableGetCaching = config.enableGetCaching,
        )
        val responseSourceInterceptor = ResponseSourceInterceptor(logger = config.logger)
        val networkPolicyInterceptor = NetworkCachePolicyInterceptor(
            keyResolver = keyResolver,
            invalidator = invalidator,
            postCacheStore = postCacheStore,
            logger = config.logger,
        )
        return RetrostashRuntime(
            keyResolver = keyResolver,
            invalidator = invalidator,
            postCacheStore = postCacheStore,
            responseSourceInterceptor = responseSourceInterceptor,
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
        builder.addInterceptor(runtime.responseSourceInterceptor)
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
