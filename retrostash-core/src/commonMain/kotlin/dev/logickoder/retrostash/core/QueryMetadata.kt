package dev.logickoder.retrostash.core

/**
 * Transport-agnostic description of a single query call, sufficient for [CoreKeyResolver] to
 * derive a stable cache key.
 *
 * @property scopeName Logical namespace — usually the API interface simple name (e.g.
 * `"UserApi"`). Prevents key collisions between unrelated APIs sharing template shapes.
 * @property template Cache template with `{placeholder}` syntax (e.g. `"users/{id}"`).
 * @property bindings Already-known placeholder values (typically extracted from `@Path` /
 * `@Query` parameters by the transport adapter).
 * @property bodyBytes Raw request body, used as a fallback source for placeholders not present
 * in [bindings] — looked up via [Utf8JsonLookup].
 * @property tagTemplates Optional list of tag templates (same `{placeholder}` syntax as
 * [template]) resolved against [bindings] / [bodyBytes] at cache-write time. The resolved tag
 * values are persisted with the entry and matched by `RetrostashStore.invalidateTag`.
 */
data class QueryMetadata(
    val scopeName: String,
    val template: String,
    val bindings: Map<String, String> = emptyMap(),
    val bodyBytes: ByteArray? = null,
    val tagTemplates: List<String> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QueryMetadata

        if (scopeName != other.scopeName) return false
        if (template != other.template) return false
        if (bindings != other.bindings) return false
        if (!bodyBytes.contentEquals(other.bodyBytes)) return false
        if (tagTemplates != other.tagTemplates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scopeName.hashCode()
        result = 31 * result + template.hashCode()
        result = 31 * result + bindings.hashCode()
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        result = 31 * result + tagTemplates.hashCode()
        return result
    }
}
