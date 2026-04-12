package dev.logickoder.retrostash.core

class RetrostashEngine(
    private val store: RetrostashStore,
    private val keyResolver: CoreKeyResolver = CoreKeyResolver(),
    private val timeoutMs: Long = 250L,
) {

    suspend fun resolveFromCache(metadata: QueryMetadata): ByteArray? {
        val key = keyResolver.resolve(metadata) ?: return null
        return withStoreTimeoutOrNull(timeoutMs) {
            store.get(key)
        }
    }

    suspend fun persistQueryResult(metadata: QueryMetadata, payload: ByteArray, maxAgeMs: Long) {
        val key = keyResolver.resolve(metadata) ?: return
        withStoreTimeoutOrNull(timeoutMs) {
            store.put(key, payload, maxAgeMs)
        }
    }

    suspend fun invalidateTemplates(templates: List<String>) {
        templates
            .filter { it.isNotBlank() }
            .forEach { template ->
                withStoreTimeoutOrNull(timeoutMs) {
                    store.invalidate(template)
                }
            }
    }

    suspend fun clearAll() {
        withStoreTimeoutOrNull(timeoutMs) {
            store.clear()
        }
    }
}