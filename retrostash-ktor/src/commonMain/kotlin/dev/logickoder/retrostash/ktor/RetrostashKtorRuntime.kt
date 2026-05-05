@file:OptIn(dev.logickoder.retrostash.internal.RetrostashInternalApi::class)

package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.internal.runMutationInvalidations

/**
 * Thin adapter that translates [RetrostashKtorMetadata] into [QueryMetadata] for the underlying
 * [RetrostashEngine]. Used by [RetrostashPlugin]; can also be used directly for testing without
 * spinning up a Ktor `HttpClient`.
 *
 * Resolves invalidation templates against bindings / bodyBytes before passing them on to
 * [RetrostashEngine.invalidateTemplates] — matches the semantics of `RetrostashOkHttpInterceptor`.
 *
 * Direct cache control — peek, update, invalidate, clear — lives on [cache]. See
 * [RetrostashKtorCache] and the README's
 * [Cache API](https://github.com/logickoder/retrostash#cache-api) section.
 */
class RetrostashKtorRuntime(
    val engine: RetrostashEngine,
    private val keyResolver: CoreKeyResolver = CoreKeyResolver(),
) {
    val cache: RetrostashKtorCache = RetrostashKtorCache(engine, keyResolver)

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
                tagTemplates = metadata.tagTemplates,
            ),
            payload = payload,
            maxAgeMs = metadata.maxAgeMs,
        )
    }

    suspend fun invalidate(metadata: RetrostashKtorMetadata) {
        engine.runMutationInvalidations(
            invalidateTemplates = metadata.invalidateTemplates,
            invalidateTagTemplates = metadata.invalidateTagTemplates,
            bindings = metadata.bindings,
            bodyBytes = metadata.bodyBytes,
        )
    }

    companion object {
        fun create(
            store: RetrostashStore,
            timeoutMs: Long = 250L,
        ): RetrostashKtorRuntime {
            val keyResolver = CoreKeyResolver()
            val engine = RetrostashEngine(
                store = store,
                keyResolver = keyResolver,
                timeoutMs = timeoutMs,
            )
            return RetrostashKtorRuntime(engine, keyResolver)
        }
    }
}
