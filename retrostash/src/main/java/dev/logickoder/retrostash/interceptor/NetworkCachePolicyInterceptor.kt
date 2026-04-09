package dev.logickoder.retrostash.interceptor

import dev.logickoder.retrostash.NetworkCacheInvalidator
import dev.logickoder.retrostash.NetworkCacheKeyResolver
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import dev.logickoder.retrostash.PostResponseCacheStore

/**
 * Applies mutation invalidation and persisted POST query caching.
 *
 * Behavior:
 * - Dirty query key: force network refresh with `Cache-Control: no-cache`.
 * - Clean POST query key with stored entry: short-circuit and return cached response.
 * - Successful mutation: mark configured keys dirty.
 * - Successful POST query response: persist body for future replay.
 */
class NetworkCachePolicyInterceptor(
    private val keyResolver: NetworkCacheKeyResolver,
    private val invalidator: NetworkCacheInvalidator,
    private val postCacheStore: PostResponseCacheStore
) : Interceptor {

    /** Intercepts requests and enforces Retrostash query/mutation policy. */
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val ctx = keyResolver.resolveQueryContext(request)
        var consumedAfterSuccessKey: String? = null

        if (ctx != null) {
            if (invalidator.isDirty(ctx.key)) {
                request = request.newBuilder()
                    .header(CacheInterceptor.HEADER_CACHE_CONTROL, "no-cache")
                    .build()
                consumedAfterSuccessKey = ctx.key
            } else if (ctx.isPost) {
                postCacheStore.get(ctx.key)?.let { cached ->
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(cached.statusCode)
                        .message(cached.statusMessage)
                        .headers(
                            Headers.Builder().apply {
                                cached.headers.forEach { (name, value) ->
                                    add(name, value)
                                }
                            }.build()
                        )
                        .body(cached.body.toResponseBody(cached.contentType?.toMediaTypeOrNull()))
                        .build()
                }
            }
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful) return response

        consumedAfterSuccessKey?.let { invalidator.clearDirty(it) }

        val mutationKeys = keyResolver.resolveMutationKeys(request)
        if (mutationKeys.isNotEmpty()) invalidator.markDirty(mutationKeys)

        if (ctx?.isPost != true) return response

        val body = response.body ?: return response
        val bytes = body.bytes()
        val headers = buildList {
            response.headers.names().forEach { name ->
                response.headers.values(name).forEach { value ->
                    add(name to value)
                }
            }
        }
        postCacheStore.put(
            key = ctx.key,
            payload = bytes,
            contentType = body.contentType()?.toString(),
            statusCode = response.code,
            statusMessage = response.message,
            headers = headers,
        )

        return response.newBuilder()
            .body(bytes.toResponseBody(body.contentType()))
            .build()
    }
}