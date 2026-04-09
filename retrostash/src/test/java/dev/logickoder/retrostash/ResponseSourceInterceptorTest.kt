package dev.logickoder.retrostash

import dev.logickoder.retrostash.interceptor.ResponseSourceInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseSourceInterceptorTest {

    @Test
    fun marks_okhttp_cache_responses() {
        val request = Request.Builder()
            .url("https://example.com/posts")
            .get()
            .build()

        val cacheResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        val networkResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("[]".toResponseBody("application/json".toMediaType()))
            .cacheResponse(cacheResponse)
            .networkResponse(networkResponse)
            .build()

        val interceptor = ResponseSourceInterceptor()
        val result = interceptor.intercept(TestInterceptorChain(request, response))

        assertEquals(
            ResponseSourceInterceptor.SOURCE_OKHTTP_VALIDATED_CACHE,
            result.header(ResponseSourceInterceptor.HEADER_RETROSTASH_SOURCE)
        )
    }

    @Test
    fun marks_network_responses() {
        val request = Request.Builder()
            .url("https://example.com/posts")
            .get()
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("[]".toResponseBody("application/json".toMediaType()))
            .build()

        val interceptor = ResponseSourceInterceptor()
        val result = interceptor.intercept(TestInterceptorChain(request, response))

        assertEquals(
            ResponseSourceInterceptor.SOURCE_NETWORK,
            result.header(ResponseSourceInterceptor.HEADER_RETROSTASH_SOURCE)
        )
    }

    @Test
    fun preserves_retrostash_cache_source_header() {
        val request = Request.Builder()
            .url("https://example.com/posts")
            .post("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header(
                ResponseSourceInterceptor.HEADER_RETROSTASH_SOURCE,
                ResponseSourceInterceptor.SOURCE_RETROSTASH_CACHE
            )
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val interceptor = ResponseSourceInterceptor()
        val result = interceptor.intercept(TestInterceptorChain(request, response))

        assertEquals(
            ResponseSourceInterceptor.SOURCE_RETROSTASH_CACHE,
            result.header(ResponseSourceInterceptor.HEADER_RETROSTASH_SOURCE)
        )
    }

}