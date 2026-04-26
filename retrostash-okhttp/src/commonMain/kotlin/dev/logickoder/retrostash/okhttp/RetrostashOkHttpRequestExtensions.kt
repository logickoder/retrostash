package dev.logickoder.retrostash.okhttp

import okhttp3.Request

fun Request.Builder.retrostash(metadata: OkHttpRetrostashMetadata): Request.Builder {
    val current = build().tag(OkHttpRetrostashMetadata::class.java)
    val merged = mergeRetrostashMetadata(current, metadata)
    return tag(OkHttpRetrostashMetadata::class.java, merged)
}

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
