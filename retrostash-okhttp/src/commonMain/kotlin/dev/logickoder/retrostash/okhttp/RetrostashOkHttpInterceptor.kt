@file:OptIn(dev.logickoder.retrostash.internal.RetrostashInternalApi::class)

package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.internal.runMutationInvalidations
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Invocation

/**
 * OkHttp [Interceptor] that powers `@CacheQuery` / `@CacheMutate` semantics.
 *
 * Reads metadata from the request via either:
 *  - An [OkHttpRetrostashMetadata] tag attached by [retrostash], [retrostashQuery], or
 *    [retrostashMutate].
 *  - A Retrofit `Invocation` tag — auto-extracted via [RetrofitMetadataExtractor].
 *
 * On a query: short-circuits to a synthetic [Response] built from the cached payload when the
 * key resolves and the entry is fresh.
 *
 * On a mutation: forwards the call to the network, then on `2xx` resolves
 * [OkHttpRetrostashMetadata.invalidateTemplates] against bindings and invalidates each.
 *
 * On any GET (when [RetrostashOkHttpConfig.enableGetCaching]): rewrites `Cache-Control` so
 * OkHttp's own disk cache can hold the body for [RetrostashOkHttpConfig.getMaxAgeSeconds]. Note
 * this rewrite is independent of Retrostash's own store — Retrostash invalidation does **not**
 * evict OkHttp HTTP cache entries. See
 * [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy).
 *
 * The synthetic cache-hit response carries a custom `X-Retrostash-Source` header so callers can
 * distinguish hit/miss without inspecting the cache directly. Possible values:
 *  - `retrostash-cache` — served from Retrostash's store.
 *  - `okhttp-cache` — served from OkHttp's HTTP cache (Retrostash store missed; consider whether
 *    you intended to layer caches — see *Caching strategy* link above).
 *  - `okhttp-validated-cache` — OkHttp HTTP cache hit revalidated with the network.
 *  - `network` — fresh network response.
 */
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
                val envelope = CachedHttpEnvelopeCodec.decode(cached)
                val replay = envelope ?: CachedHttpEnvelope(
                    payload = cached,
                    contentType = request.header("Content-Type"),
                    statusCode = 200,
                    statusMessage = "OK",
                    headers = emptyList(),
                )
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(replay.statusCode)
                    .message(replay.statusMessage)
                    .headers(
                        Headers.Builder().apply {
                            replay.headers.forEach { (name, value) -> add(name, value) }
                        }.build()
                    )
                    .addHeader(HEADER_RETROSTASH_SOURCE, SOURCE_RETROSTASH_CACHE)
                    .body(replay.payload.toResponseBody(replay.contentType?.toMediaTypeOrNull()))
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
                tagTemplates = metadata.tagTemplates,
            )
            val payload = networkResponse.peekBody(Long.MAX_VALUE).bytes()
            val envelope = CachedHttpEnvelope(
                payload = payload,
                contentType = networkResponse.body.contentType().toString(),
                statusCode = networkResponse.code,
                statusMessage = networkResponse.message,
                headers = buildList {
                    networkResponse.headers.names().forEach { name ->
                        networkResponse.headers.values(name).forEach { value ->
                            add(name to value)
                        }
                    }
                },
            )
            runBlocking {
                engine.persistQueryResult(
                    metadata = resolvedQueryMetadata,
                    payload = CachedHttpEnvelopeCodec.encode(envelope),
                    maxAgeMs = metadata.maxAgeMs,
                )
            }
        }

        if (metadata != null) {
            val hasInvalidations = metadata.invalidateTemplates.isNotEmpty()
                || metadata.invalidateTagTemplates.isNotEmpty()
            if (hasInvalidations) {
                runBlocking {
                    engine.runMutationInvalidations(
                        invalidateTemplates = metadata.invalidateTemplates,
                        invalidateTagTemplates = metadata.invalidateTagTemplates,
                        bindings = metadata.bindings,
                        bodyBytes = bodyBytes,
                    )
                }
            }
        }

        return rewriteCacheControl(markResponseSource(networkResponse), request, metadata)
    }

    private fun rewriteCacheControl(
        response: Response,
        request: okhttp3.Request,
        metadata: OkHttpRetrostashMetadata?,
    ): Response {
        val isMutation = !metadata?.invalidateTemplates.isNullOrEmpty()
            || !metadata?.invalidateTagTemplates.isNullOrEmpty()
        return when {
            isMutation -> response.newBuilder()
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

    companion object {
        private const val HEADER_RETROSTASH_SOURCE = "X-Retrostash-Source"
        private const val SOURCE_RETROSTASH_CACHE = "retrostash-cache"
        private const val SOURCE_OKHTTP_CACHE = "okhttp-cache"
        private const val SOURCE_OKHTTP_VALIDATED_CACHE = "okhttp-validated-cache"
        private const val SOURCE_NETWORK = "network"
        private const val HEADER_CACHE_CONTROL = "Cache-Control"
        private const val HEADER_PRAGMA = "Pragma"
    }
}
