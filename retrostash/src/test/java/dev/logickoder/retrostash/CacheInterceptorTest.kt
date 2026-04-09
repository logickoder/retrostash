package dev.logickoder.retrostash

import dev.logickoder.retrostash.interceptor.CacheInterceptor
import okhttp3.Call
import okhttp3.Connection
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Invocation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class CacheInterceptorTest {

    interface InterceptorApi {
        @CacheMutate(invalidate = ["posts?userId={userId}"])
        @POST("posts")
        fun mutate(@Body body: CreatePost): retrofit2.Call<Unit>

        @CacheMutate(invalidate = ["posts?userId={userId}"])
        @GET("posts/1")
        fun refresh(@Query("userId") userId: Int): retrofit2.Call<Unit>

        @GET("posts")
        fun list(@Query("userId") userId: Int): retrofit2.Call<Unit>
    }

    data class CreatePost(val userId: Int, val title: String)

    @Test
    fun cache_interceptor_sets_no_store_for_mutations() {
        val method = InterceptorApi::class.java.getMethod("mutate", CreatePost::class.java)
        val invocation = Invocation.of(method, listOf(CreatePost(1, "t")))
        val request = Request.Builder()
            .url("https://example.com/posts")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .tag(Invocation::class.java, invocation)
            .build()
        val baseResponse = baseResponse(request)

        val interceptor = CacheInterceptor()
        val result = interceptor.intercept(FakeChain(request, baseResponse))

        assertEquals("no-store", result.header("Cache-Control"))
        assertNull(result.header("Pragma"))
    }

    @Test
    fun cache_interceptor_sets_public_max_age_for_get() {
        val method = InterceptorApi::class.java.getMethod("list", Int::class.java)
        val invocation = Invocation.of(method, listOf(7))
        val request = Request.Builder()
            .url("https://example.com/posts?userId=7")
            .get()
            .tag(Invocation::class.java, invocation)
            .build()
        val baseResponse = baseResponse(request)

        val interceptor = CacheInterceptor(maxAgeSeconds = 30, enableGetCaching = true)
        val result = interceptor.intercept(FakeChain(request, baseResponse))

        assertEquals("public, max-age=30", result.header("Cache-Control"))
        assertNull(result.header("Pragma"))
    }

    @Test
    fun cache_interceptor_passthrough_for_non_get_non_mutation() {
        val request = Request.Builder()
            .url("https://example.com/other")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val baseResponse = baseResponse(request).newBuilder()
            .header("Cache-Control", "private")
            .build()

        val interceptor = CacheInterceptor()
        val result = interceptor.intercept(FakeChain(request, baseResponse))

        assertEquals("private", result.header("Cache-Control"))
    }

    @Test
    fun cache_interceptor_sets_no_store_for_get_mutations() {
        val method = InterceptorApi::class.java.getMethod("refresh", Int::class.java)
        val invocation = Invocation.of(method, listOf(1))
        val request = Request.Builder()
            .url("https://example.com/posts/1?userId=1")
            .get()
            .tag(Invocation::class.java, invocation)
            .build()
        val baseResponse = baseResponse(request)

        val interceptor = CacheInterceptor()
        val result = interceptor.intercept(FakeChain(request, baseResponse))

        assertEquals("no-store", result.header("Cache-Control"))
        assertNull(result.header("Pragma"))
    }

    private fun baseResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .header("Pragma", "no-cache")
        .header("Cache-Control", "private")
        .build()

    private class FakeChain(
        private val request: Request,
        private val response: Response,
    ) : okhttp3.Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response =
            response.newBuilder().request(request).build()

        override fun connection(): Connection? = null

        override fun call(): Call {
            throw UnsupportedOperationException("Not required for unit tests")
        }

        override fun connectTimeoutMillis(): Int = 10_000

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): okhttp3.Interceptor.Chain =
            this

        override fun readTimeoutMillis(): Int = 10_000

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): okhttp3.Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 10_000

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): okhttp3.Interceptor.Chain =
            this
    }
}
