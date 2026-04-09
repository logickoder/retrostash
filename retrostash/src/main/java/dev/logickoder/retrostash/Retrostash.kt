package dev.logickoder.retrostash

import android.content.Context
import dev.logickoder.retrostash.interceptor.CacheControlInterceptor
import dev.logickoder.retrostash.interceptor.NetworkCachePolicyInterceptor
import dev.logickoder.retrostash.interceptor.RetrostashHandleInterceptor
import dev.logickoder.retrostash.interceptor.ResponseSourceInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Entry point for integrating Retrostash with an OkHttp plus Retrofit stack.
 */
class Retrostash private constructor(
    val keyResolver: NetworkCacheKeyResolver,
    val invalidator: NetworkCacheInvalidator,
    val postCacheStore: PostResponseCacheStore,
    val responseSourceInterceptor: Interceptor,
    val cacheControlInterceptor: Interceptor,
    val networkPolicyInterceptor: NetworkCachePolicyInterceptor,
) {

    /**
     * Installs this Retrostash instance onto [builder].
     */
    fun installOn(builder: OkHttpClient.Builder): Retrostash {
        builder.addInterceptor(RetrostashHandleInterceptor(this))
        builder.addInterceptor(responseSourceInterceptor)
        builder.addInterceptor(networkPolicyInterceptor)
        builder.addNetworkInterceptor(cacheControlInterceptor)
        return this
    }

    /**
     * Marks [key] dirty and removes any persisted POST query payload for that key.
     */
    fun invalidateQueryKey(key: String): Boolean {
        if (key.isBlank()) return false
        invalidator.markDirty(listOf(key))
        postCacheStore.remove(key)
        return true
    }

    /**
     * Resolves and invalidates a query key externally using a [CacheQuery] template.
     *
     * @return resolved key when placeholders are fully bound; otherwise null.
     */
    fun invalidateQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
    ): String? {
        val key = keyResolver.buildQueryKey(apiClass, template, bindings) ?: return null
        invalidateQueryKey(key)
        return key
    }

    /** Clears persisted and in-memory state for this instance. */
    fun clear() {
        invalidator.clear()
        postCacheStore.clear()
    }

    companion object {

        /**
         * Creates Retrostash runtime components without mutating an OkHttp builder.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            config: RetrostashConfig = RetrostashConfig(),
        ): Retrostash {
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
            return Retrostash(
                keyResolver = keyResolver,
                invalidator = invalidator,
                postCacheStore = postCacheStore,
                responseSourceInterceptor = responseSourceInterceptor,
                cacheControlInterceptor = cacheControlInterceptor,
                networkPolicyInterceptor = networkPolicyInterceptor,
            )
        }

        /**
         * Creates and installs Retrostash interceptors on [builder].
         */
        @JvmStatic
        @JvmOverloads
        fun install(
            builder: OkHttpClient.Builder,
            context: Context,
            config: RetrostashConfig = RetrostashConfig(),
        ): Retrostash {
            return create(context, config).installOn(builder)
        }

        /**
         * Returns Retrostash instance attached to [client], if present.
         */
        @JvmStatic
        fun from(client: OkHttpClient): Retrostash? {
            return client.interceptors
                .asSequence()
                .filterIsInstance<RetrostashHandleInterceptor>()
                .map { it.retrostash }
                .firstOrNull()
        }

        /**
         * Clears all persisted Retrostash state for [context].
         */
        @JvmStatic
        fun clear(context: Context) {
            NetworkCacheInvalidator.clear(context)
            PostResponseCacheStore.clear(context)
        }
    }
}
