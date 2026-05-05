package dev.logickoder.retrostash.internal

import dev.logickoder.retrostash.core.RetrostashEngine

/**
 * Resolves mutation-side invalidation templates (key + tag) against [bindings] / [bodyBytes]
 * and dispatches to the engine. Shared between `RetrostashOkHttpInterceptor` (post-2xx
 * response) and `RetrostashKtorRuntime.invalidate` (post-2xx response).
 *
 * Marked [RetrostashInternalApi] — not a public API.
 */
@RetrostashInternalApi
suspend fun RetrostashEngine.runMutationInvalidations(
    invalidateTemplates: List<String>,
    invalidateTagTemplates: List<String>,
    bindings: Map<String, String>,
    bodyBytes: ByteArray?,
) {
    if (invalidateTemplates.isNotEmpty()) {
        val resolved = invalidateTemplates.mapNotNull { template ->
            TemplateResolver.resolve(template, bindings, bodyBytes)
        }
        if (resolved.isNotEmpty()) invalidateTemplates(resolved)
    }
    if (invalidateTagTemplates.isNotEmpty()) {
        val resolved = invalidateTagTemplates.mapNotNull { template ->
            TemplateResolver.resolve(template, bindings, bodyBytes)
        }
        if (resolved.isNotEmpty()) invalidateTags(resolved)
    }
}
