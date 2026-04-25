package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.QueryMetadata
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class RetrostashOkHttpBridge(
    val store: RetrostashStore,
    private val config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
    private val keyResolver: CoreKeyResolver = CoreKeyResolver(),
    val engine: RetrostashEngine = RetrostashEngine(
        store = store,
        keyResolver = keyResolver,
        timeoutMs = config.timeoutMs,
    ),
) {
    fun install(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.addInterceptor(RetrostashOkHttpHandleInterceptor(this))
        builder.addInterceptor(RetrostashOkHttpInterceptor(engine, config))
        return builder
    }

    fun invalidateQueryKey(key: String): Boolean {
        if (key.isBlank()) return false
        runBlocking {
            engine.invalidateTemplates(listOf(key))
        }
        return true
    }

    fun invalidateQuery(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
    ): String? {
        val resolved = keyResolver.resolve(
            QueryMetadata(
                scopeName = apiClass.simpleName,
                template = template,
                bindings = bindings.mapNotNull { (k, v) ->
                    v?.toString()?.let { value -> k to value }
                }.toMap(),
            )
        ) ?: return null

        invalidateQueryKey(resolved)
        return resolved
    }

    companion object {
        fun from(client: OkHttpClient): RetrostashOkHttpBridge? {
            return client.interceptors
                .asSequence()
                .filterIsInstance<RetrostashOkHttpHandleInterceptor>()
                .map { it.bridge }
                .firstOrNull()
        }
    }
}
