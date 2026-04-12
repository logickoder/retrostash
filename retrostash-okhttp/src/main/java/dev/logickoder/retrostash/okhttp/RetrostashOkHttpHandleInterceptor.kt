package dev.logickoder.retrostash.okhttp

import okhttp3.Interceptor
import okhttp3.Response

internal class RetrostashOkHttpHandleInterceptor(
    val bridge: RetrostashOkHttpBridge,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
