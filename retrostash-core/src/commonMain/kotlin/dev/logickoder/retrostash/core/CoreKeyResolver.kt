package dev.logickoder.retrostash.core

/**
 * Builds a stable cache key from [QueryMetadata].
 *
 * Resolution order for each `{placeholder}` in [QueryMetadata.template]:
 *  1. [QueryMetadata.bindings] — populated from `@Path` / `@Query` by the transport adapter.
 *  2. [QueryMetadata.bodyBytes] — JSON body searched for a matching primitive field via
 *     [Utf8JsonLookup].
 *  3. Unresolved → returns `null`. Caller skips the cache action safely.
 *
 * Output shape: `scopeName|<resolvedTemplate>|<fingerprint-hash>` where `fingerprint` is a
 * deterministic 64-bit FNV-style hash of the sorted `key=value` bindings, hex-encoded.
 */
class CoreKeyResolver {

    /**
     * Resolves [metadata] into a cache key, or `null` if any placeholder cannot be filled from
     * [QueryMetadata.bindings] or [QueryMetadata.bodyBytes].
     */
    fun resolve(metadata: QueryMetadata): String? {
        if (metadata.template.isBlank()) return null

        val placeholders = placeholdersOf(metadata.template)
        val resolvedBindings = resolveBindings(placeholders, metadata)
            ?: return null

        return buildKey(metadata.scopeName, metadata.template, placeholders, resolvedBindings)
    }

    /**
     * Resolves [QueryMetadata.tagTemplates] against the same bindings / body sources used by
     * [resolve]. Templates that cannot be fully resolved are dropped silently.
     */
    fun resolveTags(metadata: QueryMetadata): List<String> {
        if (metadata.tagTemplates.isEmpty()) return emptyList()
        return metadata.tagTemplates.mapNotNull { template ->
            resolveTemplate(template, metadata)
        }
    }

    private fun resolveTemplate(template: String, metadata: QueryMetadata): String? {
        if (template.isBlank()) return null
        val placeholders = placeholdersOf(template)
        if (placeholders.isEmpty()) return template

        val resolved = resolveBindings(placeholders, metadata) ?: return null
        return PLACEHOLDER_REGEX.replace(template) { resolved[it.groupValues[1]].orEmpty() }
    }

    private fun resolveBindings(
        placeholders: List<String>,
        metadata: QueryMetadata,
    ): Map<String, String>? {
        if (placeholders.isEmpty()) return emptyMap()
        val working = metadata.bindings.toMutableMap()
        placeholders.forEach { placeholder ->
            if (working.containsKey(placeholder)) return@forEach
            val value = metadata.bodyBytes?.let {
                Utf8JsonLookup.findFirstPrimitiveByKey(it, placeholder)
            } ?: return@forEach
            working[placeholder] = value
        }
        if (!placeholders.all { working.containsKey(it) }) return null
        return working
    }

    private fun buildKey(
        scopeName: String,
        template: String,
        placeholders: List<String>,
        bindings: Map<String, String>,
    ): String {
        val resolved = PLACEHOLDER_REGEX.replace(template) { bindings[it.groupValues[1]].orEmpty() }
        val fingerprint = placeholders.sorted()
            .joinToString("|") { "$it=${bindings[it].orEmpty()}" }

        return "$scopeName|$resolved|${stableHash64(fingerprint)}"
    }

    private fun placeholdersOf(template: String): List<String> =
        PLACEHOLDER_REGEX.findAll(template)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

    private fun stableHash64(value: String): String {
        var hash = -0x340d631b8c4678f3L
        value.encodeToByteArray().forEach { byte ->
            hash = hash xor (byte.toLong() and 0xff)
            hash *= 0x100000001b3L
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")
    }
}
