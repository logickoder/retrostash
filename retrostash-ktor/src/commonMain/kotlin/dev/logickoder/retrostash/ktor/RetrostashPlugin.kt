package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders

/**
 * Ktor `HttpClient` plugin that adds annotation-style query caching and mutation invalidation.
 *
 * Installs request/response hooks that:
 *  - On request: look up [RetrostashKtorMetadata.queryTemplate] in the store. If present, mark
 *    the call as a cache hit (sets `Cache-Control: only-if-cached` + custom headers) and stash
 *    the cached payload under [RetrostashCachedPayloadKey] for the caller to read.
 *  - On response: skip on non-2xx. Otherwise invalidate any
 *    [RetrostashKtorMetadata.invalidateTemplates] (resolved against bindings) and persist the
 *    response body when the metadata declares a query template + `maxAgeMs > 0`.
 *
 * Wire metadata onto a request with [retrostashQuery] / [retrostashMutate].
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(RetrostashPlugin) {
 *         store = InMemoryRetrostashStore()
 *         timeoutMs = 250
 *     }
 * }
 *
 * client.get("https://api.example.com/feed/7") {
 *     retrostashQuery(
 *         scopeName = "FeedApi",
 *         template = "feed/{id}",
 *         bindings = mapOf("id" to "7"),
 *         maxAgeMs = 60_000L,
 *     )
 * }
 * ```
 */
val RetrostashPlugin = createClientPlugin("Retrostash", ::RetrostashConfig) {
	val configuredStore = pluginConfig.store ?: return@createClientPlugin
    val log = pluginConfig.logger
	val runtime = RetrostashKtorRuntime(
		engine = RetrostashEngine(
			store = configuredStore,
			keyResolver = CoreKeyResolver(),
			timeoutMs = pluginConfig.timeoutMs,
		)
	)

	onRequest { request, _ ->
		val metadata = request.attributes.getOrNull(RetrostashMetadataKey) ?: return@onRequest
        val cached = runtime.resolveFromCache(metadata) ?: run {
            log?.invoke("retrostash: miss ${metadata.scopeName} ${metadata.queryTemplate}")
            return@onRequest
        }

		markCacheHit(request.headers)
		request.attributes.put(RetrostashCachedPayloadKey, cached)
		request.headers.append("x-retrostash-cache-size", cached.size.toString())
        log?.invoke("retrostash: hit ${metadata.scopeName} ${metadata.queryTemplate} (${cached.size}B)")
	}

	onResponse { response ->
		val request = response.call.request
		val metadata = request.attributes.getOrNull(RetrostashMetadataKey) ?: return@onResponse

		if (response.status.value !in 200..299) return@onResponse

        if (metadata.invalidateTemplates.isNotEmpty()) {
            runtime.invalidate(metadata)
            log?.invoke("retrostash: invalidated ${metadata.invalidateTemplates}")
        }

        if (metadata.queryTemplate != null && metadata.maxAgeMs > 0L) {
            val saved = response.call.save()
            val payload = saved.response.body<ByteArray>()
            runtime.persistQueryResult(metadata, payload)
            log?.invoke("retrostash: persisted ${metadata.scopeName} ${metadata.queryTemplate} (${payload.size}B)")
        }
	}
}

private fun markCacheHit(headers: HeadersBuilder) {
	if (headers[HttpHeaders.CacheControl].isNullOrBlank()) {
		headers.append(HttpHeaders.CacheControl, "only-if-cached")
	}
	headers.append("x-retrostash-cache", "hit")
}
