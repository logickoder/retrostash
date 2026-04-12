package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore

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
        if (metadata.invalidateTemplates.isNotEmpty()) {
            engine.invalidateTemplates(metadata.invalidateTemplates)
        }
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