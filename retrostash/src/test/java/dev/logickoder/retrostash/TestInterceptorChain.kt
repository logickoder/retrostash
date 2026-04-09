package dev.logickoder.retrostash

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

internal class TestInterceptorChain(
    private val requestValue: Request,
    private val responseValue: Response,
) : Interceptor.Chain {
    override fun request(): Request = requestValue

    override fun proceed(request: Request): Response =
        responseValue.newBuilder().request(request).build()

    override fun connection(): Connection? = null

    override fun call(): Call {
        throw UnsupportedOperationException("Not required for unit tests")
    }

    override fun connectTimeoutMillis(): Int = 10_000

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun readTimeoutMillis(): Int = 10_000

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun writeTimeoutMillis(): Int = 10_000

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}