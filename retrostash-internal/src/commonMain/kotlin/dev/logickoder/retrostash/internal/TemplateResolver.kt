package dev.logickoder.retrostash.internal

import dev.logickoder.retrostash.core.Utf8JsonLookup

/**
 * Substitutes `{placeholder}` tokens in a template against bindings, with optional JSON-body
 * fallback. Shared by `RetrostashOkHttpInterceptor`, `RetrostashKtorRuntime`, and the cache
 * surfaces (`RetrostashOkHttpCache`, `RetrostashKtorCache`).
 *
 * Marked [RetrostashInternalApi] — not a public API; signature may change without notice.
 */
@RetrostashInternalApi
object TemplateResolver {
    private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")

    /**
     * Substitutes `{placeholder}` tokens in [template] from [bindings], falling back to a JSON
     * field lookup against [bodyBytes]. Returns `null` if any placeholder is unresolved.
     *
     * Templates without placeholders are returned verbatim.
     */
    fun resolve(
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
                    val fromBody = bodyBytes?.let {
                        Utf8JsonLookup.findFirstPrimitiveByKey(it, placeholder)
                    }
                    if (fromBody != null) working[placeholder] = fromBody
                }
            }
        val unresolved = PLACEHOLDER_REGEX.findAll(template)
            .any { !working.containsKey(it.groupValues[1]) }
        if (unresolved) return null
        return PLACEHOLDER_REGEX.replace(template) { working[it.groupValues[1]].orEmpty() }
    }
}
