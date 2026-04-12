package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders

val RetrostashPlugin = createClientPlugin("Retrostash", ::RetrostashConfig) {
	val configuredStore = pluginConfig.store ?: return@createClientPlugin
	val engine = RetrostashEngine(
		store = configuredStore,
		keyResolver = CoreKeyResolver(),
		timeoutMs = pluginConfig.timeoutMs,
	)

	onRequest { request, _ ->
		val metadata = request.attributes.getOrNull(RetrostashMetadataKey) ?: return@onRequest
		val template = metadata.queryTemplate ?: return@onRequest

		val queryMetadata = QueryMetadata(
			scopeName = metadata.scopeName,
			template = template,
			bindings = metadata.bindings,
			bodyBytes = metadata.bodyBytes,
		)
		val cached = engine.resolveFromCache(queryMetadata) ?: return@onRequest

		markCacheHit(request.headers)
		if (cached.isNotEmpty()) {
			request.headers.append("x-retrostash-cache-size", cached.size.toString())
		}
	}

	onResponse { response ->
		val request = response.call.request
		val metadata = request.attributes.getOrNull(RetrostashMetadataKey) ?: return@onResponse

		if (metadata.invalidateTemplates.isNotEmpty()) {
			engine.invalidateTemplates(metadata.invalidateTemplates)
		}

		if (response.status.value !in 200..299) return@onResponse
	}
}

private fun markCacheHit(headers: HeadersBuilder) {
	if (headers[HttpHeaders.CacheControl].isNullOrBlank()) {
		headers.append(HttpHeaders.CacheControl, "only-if-cached")
	}
	headers.append("x-retrostash-cache", "hit")
}
