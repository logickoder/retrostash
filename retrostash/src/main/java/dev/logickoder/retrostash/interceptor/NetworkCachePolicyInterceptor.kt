package dev.logickoder.retrostash.interceptor

import dev.logickoder.retrostash.NetworkCacheInvalidator
import dev.logickoder.retrostash.NetworkCacheKeyResolver
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import dev.logickoder.retrostash.PostResponseCacheStore

/**
 * Applies annotation-driven invalidation and persisted POST query response caching.
 */
class NetworkCachePolicyInterceptor(
    private val keyResolver: NetworkCacheKeyResolver,
    private val invalidator: NetworkCacheInvalidator,
    private val postCacheStore: PostResponseCacheStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val ctx = keyResolver.resolveQueryContext(request)

        if (ctx != null) {
            if (invalidator.consumeIfDirty(ctx.key)) {
                request = request.newBuilder()
                    .header(CacheInterceptor.HEADER_CACHE_CONTROL, "no-cache")
                    .build()
            } else if (ctx.isPost) {
                postCacheStore.get(ctx.key)?.let { cached ->
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(cached.body.toResponseBody(cached.contentType?.toMediaTypeOrNull()))
                        .build()
                }
            }
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful) return response

        val mutationKeys = keyResolver.resolveMutationKeys(request)
        if (mutationKeys.isNotEmpty()) invalidator.markDirty(mutationKeys)

        if (ctx?.isPost != true) return response

        val body = response.body ?: return response
        val bytes = body.bytes()
        postCacheStore.put(ctx.key, bytes, body.contentType()?.toString())

        return response.newBuilder()
            .body(bytes.toResponseBody(body.contentType()))
            .build()
    }
}