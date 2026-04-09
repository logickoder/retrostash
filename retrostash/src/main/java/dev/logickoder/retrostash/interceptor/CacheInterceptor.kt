package dev.logickoder.retrostash.interceptor

import dev.logickoder.retrostash.CacheMutate
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

internal class CacheInterceptor(
    private val maxAgeSeconds: Long = 24 * 60 * 60L,
    private val enableGetCaching: Boolean = true
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val method = request.tag(Invocation::class.java)?.method()
        return when {
            method?.isAnnotationPresent(CacheMutate::class.java) == true -> {
                // Prevent OkHttp from caching mutation responses regardless of server headers.
                chain.proceed(request).newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .header(HEADER_CACHE_CONTROL, "no-store")
                    .build()
            }

            request.method == "GET" && enableGetCaching -> {
                // POST @CacheQuery is handled by PostResponseCacheStore; OkHttp never caches POST.
                chain.proceed(request).newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .header(HEADER_CACHE_CONTROL, "public, max-age=$maxAgeSeconds")
                    .build()
            }

            else -> chain.proceed(request) // Non-annotated: let server headers flow through.
        }
    }

    companion object {
        internal const val HEADER_CACHE_CONTROL = "Cache-Control"
        internal const val HEADER_PRAGMA = "Pragma"
    }
}