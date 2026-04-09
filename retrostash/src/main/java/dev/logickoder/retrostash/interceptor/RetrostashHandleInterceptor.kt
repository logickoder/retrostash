package dev.logickoder.retrostash.interceptor

import dev.logickoder.retrostash.Retrostash
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Stores Retrostash instance on the OkHttp interceptor chain for runtime retrieval.
 */
internal class RetrostashHandleInterceptor(
    val retrostash: Retrostash,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}