package dev.logickoder.retrostash.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Marks the origin of a response so callers can distinguish Retrostash, OkHttp cache, and network.
 */
internal class ResponseSourceInterceptor(
    private val logger: ((String) -> Unit)? = null,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val source = response.header(HEADER_RETROSTASH_SOURCE) ?: when {
            response.cacheResponse != null && response.networkResponse == null -> SOURCE_OKHTTP_CACHE
            response.cacheResponse != null && response.networkResponse != null -> SOURCE_OKHTTP_VALIDATED_CACHE
            response.networkResponse != null -> SOURCE_NETWORK
            else -> SOURCE_NETWORK
        }

        log("response source -> $source for ${request.method} ${request.url}")

        if (response.header(HEADER_RETROSTASH_SOURCE) != null) return response

        return response.newBuilder()
            .header(HEADER_RETROSTASH_SOURCE, source)
            .build()
    }

    private fun log(message: String) {
        logger?.invoke("[Retrostash] $message")
    }

    companion object {
        internal const val HEADER_RETROSTASH_SOURCE = "X-Retrostash-Source"
        internal const val SOURCE_RETROSTASH_CACHE = "retrostash-cache"
        internal const val SOURCE_OKHTTP_CACHE = "okhttp-cache"
        internal const val SOURCE_OKHTTP_VALIDATED_CACHE = "okhttp-validated-cache"
        internal const val SOURCE_NETWORK = "network"
    }
}