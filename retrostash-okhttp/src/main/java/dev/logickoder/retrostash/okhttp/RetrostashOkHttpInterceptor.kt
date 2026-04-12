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
    private val config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
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
                    .addHeader(HEADER_RETROSTASH_SOURCE, SOURCE_RETROSTASH_CACHE)
                    .body(cached.toResponseBody(detectContentType(request)))
                    .build()
            }
        }

        val networkResponse = chain.proceed(request)
        if (!networkResponse.isSuccessful) {
            return markResponseSource(networkResponse)
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

        return rewriteCacheControl(markResponseSource(networkResponse), request, metadata)
    }

    private fun rewriteCacheControl(
        response: Response,
        request: okhttp3.Request,
        metadata: OkHttpRetrostashMetadata?,
    ): Response {
        return when {
            !metadata?.invalidateTemplates.isNullOrEmpty() -> response.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .removeHeader(HEADER_CACHE_CONTROL)
                .header(HEADER_CACHE_CONTROL, "no-store")
                .build()

            request.method == "GET" && config.enableGetCaching -> response.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .removeHeader(HEADER_CACHE_CONTROL)
                .header(HEADER_CACHE_CONTROL, "public, max-age=${config.getMaxAgeSeconds}")
                .build()

            else -> response
        }
    }

    private fun markResponseSource(response: Response): Response {
        if (!response.header(HEADER_RETROSTASH_SOURCE).isNullOrBlank()) return response

        val source = when {
            response.cacheResponse != null && response.networkResponse == null -> SOURCE_OKHTTP_CACHE
            response.cacheResponse != null && response.networkResponse != null -> SOURCE_OKHTTP_VALIDATED_CACHE
            response.networkResponse != null -> SOURCE_NETWORK
            else -> SOURCE_NETWORK
        }

        config.logger?.invoke("[Retrostash] response source -> $source for ${response.request.method} ${response.request.url}")
        return response.newBuilder()
            .header(HEADER_RETROSTASH_SOURCE, source)
            .build()
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
        private const val HEADER_RETROSTASH_SOURCE = "X-Retrostash-Source"
        private const val SOURCE_RETROSTASH_CACHE = "retrostash-cache"
        private const val SOURCE_OKHTTP_CACHE = "okhttp-cache"
        private const val SOURCE_OKHTTP_VALIDATED_CACHE = "okhttp-validated-cache"
        private const val SOURCE_NETWORK = "network"
        private const val HEADER_CACHE_CONTROL = "Cache-Control"
        private const val HEADER_PRAGMA = "Pragma"
        private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")
    }
}
