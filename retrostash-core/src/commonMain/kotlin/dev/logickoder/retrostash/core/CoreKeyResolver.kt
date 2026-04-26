package dev.logickoder.retrostash.core

class CoreKeyResolver {

    fun resolve(metadata: QueryMetadata): String? {
        if (metadata.template.isBlank()) return null

        val placeholders = PLACEHOLDER_REGEX.findAll(metadata.template)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        if (placeholders.isEmpty()) {
            return buildKey(metadata.scopeName, metadata.template, emptyMap())
        }

        val resolvedBindings = metadata.bindings.toMutableMap()
        val unresolved = placeholders.filterNot { resolvedBindings.containsKey(it) }
        if (unresolved.isNotEmpty()) {
            unresolved.forEach { placeholder ->
                val value = metadata.bodyBytes?.let {
                    Utf8JsonLookup.findFirstPrimitiveByKey(
                        it,
                        placeholder
                    )
                }
                if (value != null) {
                    resolvedBindings[placeholder] = value
                }
            }
        }

        if (!placeholders.all { resolvedBindings.containsKey(it) }) return null
        return buildKey(metadata.scopeName, metadata.template, resolvedBindings)
    }

    private fun buildKey(
        scopeName: String,
        template: String,
        bindings: Map<String, String>
    ): String {
        val placeholders = PLACEHOLDER_REGEX.findAll(template)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val resolved = PLACEHOLDER_REGEX.replace(template) { bindings[it.groupValues[1]].orEmpty() }
        val fingerprint = placeholders.sorted()
            .joinToString("|") { "$it=${bindings[it].orEmpty()}" }

        return "$scopeName|$resolved|${stableHash64(fingerprint)}"
    }

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
