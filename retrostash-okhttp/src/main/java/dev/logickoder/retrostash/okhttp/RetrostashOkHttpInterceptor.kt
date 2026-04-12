package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.Utf8JsonLookup
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Invocation

class RetrostashOkHttpInterceptor(
    private val engine: RetrostashEngine,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val metadata = request.tag(OkHttpRetrostashMetadata::class.java)
            ?: request.tag(Invocation::class.java)?.let(RetrofitMetadataExtractor::extract)

        val queryTemplate = metadata?.queryTemplate
        if (!queryTemplate.isNullOrBlank()) {
            val bodyBytes = requestBodyBytes(request)
            val resolvedQueryMetadata = QueryMetadata(
                scopeName = metadata.scopeName,
                template = queryTemplate,
                bindings = metadata.bindings,
                bodyBytes = bodyBytes,
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

        val bodyBytes = requestBodyBytes(request)
        if (!queryTemplate.isNullOrBlank()) {
            val resolvedQueryMetadata = QueryMetadata(
                scopeName = metadata.scopeName,
                template = queryTemplate,
                bindings = metadata.bindings,
                bodyBytes = bodyBytes,
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
            val resolvedInvalidations = metadata.invalidateTemplates.mapNotNull { template ->
                resolveTemplate(template, metadata.bindings, bodyBytes)
            }
            runBlocking {
                engine.invalidateTemplates(resolvedInvalidations)
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

    private fun resolveTemplate(
        template: String,
        bindings: Map<String, String>,
        bodyBytes: ByteArray?,
    ): String? {
        if (!template.contains('{')) return template
        val working = bindings.toMutableMap()
        PLACEHOLDER_REGEX.findAll(template)
            .map { it.groupValues[1] }
            .distinct()
            .forEach { placeholder ->
                if (!working.containsKey(placeholder)) {
                    val fromBody =
                        bodyBytes?.let { Utf8JsonLookup.findFirstPrimitiveByKey(it, placeholder) }
                    if (fromBody != null) {
                        working[placeholder] = fromBody
                    }
                }
            }

        val unresolved =
            PLACEHOLDER_REGEX.findAll(template).any { !working.containsKey(it.groupValues[1]) }
        if (unresolved) return null

        return PLACEHOLDER_REGEX.replace(template) { working[it.groupValues[1]].orEmpty() }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")
    }
}
