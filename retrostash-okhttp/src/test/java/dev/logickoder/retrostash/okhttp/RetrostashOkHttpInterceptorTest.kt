package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import dev.logickoder.retrostash.core.RetrostashEngine
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetrostashOkHttpInterceptorTest {

    @Test
    fun rewrites_get_cache_control_and_marks_network_source() {
        val interceptor = RetrostashOkHttpInterceptor(
            engine = RetrostashEngine(InMemoryRetrostashStore()),
            config = RetrostashOkHttpConfig(getMaxAgeSeconds = 120L, enableGetCaching = true),
        )

        val request = Request.Builder().url("https://example.com/users").get().build()
        val chain = FakeChain(request) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        val response = interceptor.intercept(chain)

        assertEquals("public, max-age=120", response.header("Cache-Control"))
        assertEquals("network", response.header("X-Retrostash-Source"))
    }

    @Test
    fun mutation_response_sets_no_store() {
        val interceptor = RetrostashOkHttpInterceptor(
            engine = RetrostashEngine(InMemoryRetrostashStore()),
            config = RetrostashOkHttpConfig(),
        )

        val request = Request.Builder()
            .url("https://example.com/users/42")
            .post(EmptyRequestBody)
            .retrostashMutate(
                scopeName = "UserApi",
                invalidateTemplates = listOf("users/{id}"),
                bindings = mapOf("id" to "42"),
            )
            .build()

        val chain = FakeChain(request) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        val response = interceptor.intercept(chain)
        assertEquals("no-store", response.header("Cache-Control"))
    }

    private class FakeChain(
        private val request: Request,
        private val proceedBlock: (Request) -> Response,
    ) : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response = proceedBlock(request)

        override fun call(): Call {
            throw UnsupportedOperationException("Not needed in unit test")
        }

        override fun connection(): Connection? = null

        override fun connectTimeoutMillis(): Int = 10_000

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 10_000

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 10_000

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private object EmptyRequestBody : okhttp3.RequestBody() {
        override fun contentType() = "application/json".toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {
            sink.write(Buffer().writeUtf8("{}"), 2)
        }
    }
}
