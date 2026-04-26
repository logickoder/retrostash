package dev.logickoder.retrostash.okhttp

import okhttp3.Request

/**
 * Attaches or merges [metadata] onto this `Request.Builder` as an OkHttp tag. Existing tag
 * fields are preserved when incoming values are blank/null/empty.
 */
fun Request.Builder.retrostash(metadata: OkHttpRetrostashMetadata): Request.Builder {
    val current = build().tag(OkHttpRetrostashMetadata::class.java)
    val merged = mergeRetrostashMetadata(current, metadata)
    return tag(OkHttpRetrostashMetadata::class.java, merged)
}

/**
 * Marks this request as a Retrostash query. Equivalent to `@CacheQuery` for direct OkHttp use.
 *
 * @param scopeName Logical namespace (typically the API class name).
 * @param template Cache template (e.g. `"users/{id}"`).
 * @param bindings Placeholder values from the call site.
 * @param maxAgeMs TTL in milliseconds. `0` disables persistence (lookup-only).
 */
fun Request.Builder.retrostashQuery(
    scopeName: String,
    template: String,
    bindings: Map<String, String> = emptyMap(),
    maxAgeMs: Long = 0L,
): Request.Builder {
    val metadata = OkHttpRetrostashMetadata(
        scopeName = scopeName,
        queryTemplate = template,
        bindings = bindings,
        maxAgeMs = maxAgeMs,
    )
    return retrostash(metadata)
}

/**
 * Marks this request as a Retrostash mutation. Equivalent to `@CacheMutate` for direct OkHttp
 * use. On a `2xx` response, [RetrostashOkHttpInterceptor] resolves [invalidateTemplates] against
 * [bindings] and clears matching cache entries.
 */
fun Request.Builder.retrostashMutate(
    scopeName: String,
    invalidateTemplates: List<String>,
    bindings: Map<String, String> = emptyMap(),
): Request.Builder {
    val metadata = OkHttpRetrostashMetadata(
        scopeName = scopeName,
        bindings = bindings,
        invalidateTemplates = invalidateTemplates,
    )
    return retrostash(metadata)
}

private fun mergeRetrostashMetadata(
    current: OkHttpRetrostashMetadata?,
    incoming: OkHttpRetrostashMetadata,
): OkHttpRetrostashMetadata {
    if (current == null) return incoming

    val scope = incoming.scopeName.ifBlank { current.scopeName }
    val queryTemplate = incoming.queryTemplate ?: current.queryTemplate
    val maxAgeMs = if (incoming.maxAgeMs > 0L) incoming.maxAgeMs else current.maxAgeMs
    val bindings = if (incoming.bindings.isNotEmpty()) {
        current.bindings + incoming.bindings
    } else {
        current.bindings
    }
    val invalidates = (current.invalidateTemplates + incoming.invalidateTemplates).distinct()

    return OkHttpRetrostashMetadata(
        scopeName = scope,
        queryTemplate = queryTemplate,
        maxAgeMs = maxAgeMs,
        bindings = bindings,
        invalidateTemplates = invalidates,
    )
}
