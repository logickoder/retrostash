package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.core.Utf8JsonLookup

private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")

class RetrostashKtorRuntime(
    val engine: RetrostashEngine,
) {
    suspend fun resolveFromCache(metadata: RetrostashKtorMetadata): ByteArray? {
        val template = metadata.queryTemplate ?: return null
        return engine.resolveFromCache(
            QueryMetadata(
                scopeName = metadata.scopeName,
                template = template,
                bindings = metadata.bindings,
                bodyBytes = metadata.bodyBytes,
            )
        )
    }

    suspend fun persistQueryResult(metadata: RetrostashKtorMetadata, payload: ByteArray) {
        val template = metadata.queryTemplate ?: return
        engine.persistQueryResult(
            metadata = QueryMetadata(
                scopeName = metadata.scopeName,
                template = template,
                bindings = metadata.bindings,
                bodyBytes = metadata.bodyBytes,
            ),
            payload = payload,
            maxAgeMs = metadata.maxAgeMs,
        )
    }

    suspend fun invalidate(metadata: RetrostashKtorMetadata) {
        if (metadata.invalidateTemplates.isEmpty()) return
        val resolved = metadata.invalidateTemplates.mapNotNull { template ->
            resolveTemplate(template, metadata.bindings, metadata.bodyBytes)
        }
        if (resolved.isNotEmpty()) {
            engine.invalidateTemplates(resolved)
        }
    }

    private fun resolveTemplate(
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

    companion object {
        fun create(
            store: RetrostashStore,
            timeoutMs: Long = 250L,
        ): RetrostashKtorRuntime {
            val engine = RetrostashEngine(
                store = store,
                keyResolver = CoreKeyResolver(),
                timeoutMs = timeoutMs,
            )
            return RetrostashKtorRuntime(engine)
        }
    }
}