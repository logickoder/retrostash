package dev.logickoder.retrostash

import dev.logickoder.retrostash.interceptor.NetworkCachePolicyInterceptor
import okhttp3.Interceptor

/**
 * Runtime components created by Retrostash and installable into OkHttp.
 *
 * @property keyResolver request key resolver for query and mutation templates.
 * @property invalidator mutation invalidation index.
 * @property postCacheStore persistent store for POST query responses.
 * @property cacheControlInterceptor network interceptor that rewrites cache headers.
 * @property networkPolicyInterceptor application interceptor that applies cache/invalidation policy.
 */
data class RetrostashRuntime(
    val keyResolver: NetworkCacheKeyResolver,
    val invalidator: NetworkCacheInvalidator,
    val postCacheStore: PostResponseCacheStore,
    val cacheControlInterceptor: Interceptor,
    val networkPolicyInterceptor: NetworkCachePolicyInterceptor,
)
