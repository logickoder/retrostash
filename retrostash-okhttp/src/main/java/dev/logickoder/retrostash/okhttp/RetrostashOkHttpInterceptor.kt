package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class RetrostashOkHttpInterceptor(
    private val engine: RetrostashEngine,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val metadata = request.tag(OkHttpRetrostashMetadata::class.java)

        val queryTemplate = metadata?.queryTemplate
        if (!queryTemplate.isNullOrBlank()) {
            val resolvedQueryMetadata = QueryMetadata(
                scopeName = metadata.scopeName,
                template = queryTemplate,
                bindings = metadata.bindings,
                bodyBytes = requestBodyBytes(request),
            )

            val cached = runBlocking {
                engine.resolveFromCache(resolvedQueryMetadata)
            }
            if (cached != null) {
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .addHeader("x-retrostash-source", "cache")
                    .body(cached.toResponseBody(detectContentType(request)))
                    .build()
            }
        }

        val networkResponse = chain.proceed(request)
        if (!networkResponse.isSuccessful) {
            return networkResponse
        }

        if (!queryTemplate.isNullOrBlank()) {
            val resolvedQueryMetadata = QueryMetadata(
                scopeName = metadata.scopeName,
                template = queryTemplate,
                bindings = metadata.bindings,
                bodyBytes = requestBodyBytes(request),
            )
            val payload = networkResponse.peekBody(Long.MAX_VALUE).bytes()
            runBlocking {
                engine.persistQueryResult(
                    metadata = resolvedQueryMetadata,
                    payload = payload,
                    maxAgeMs = metadata.maxAgeMs,
                )
            }
        }

        if (!metadata?.invalidateTemplates.isNullOrEmpty()) {
            runBlocking {
                engine.invalidateTemplates(metadata.invalidateTemplates)
            }
        }

        return networkResponse
    }

    private fun detectContentType(request: okhttp3.Request) =
        request.header("Content-Type")?.toMediaTypeOrNull()
            ?: "application/json; charset=utf-8".toMediaTypeOrNull()

    private fun requestBodyBytes(request: okhttp3.Request): ByteArray? =
        runCatching {
            val body = request.body ?: return null
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        }.getOrNull()
}
