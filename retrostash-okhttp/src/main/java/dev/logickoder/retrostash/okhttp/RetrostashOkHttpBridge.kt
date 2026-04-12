package dev.logickoder.retrostash.okhttp

import android.content.Context
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
        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
        ): RetrostashOkHttpBridge {
            val store = AndroidRetrostashStore(context, config)
            val keyResolver = CoreKeyResolver()
            val engine = RetrostashEngine(
                store = store,
                keyResolver = keyResolver,
                timeoutMs = config.timeoutMs,
            )
            return RetrostashOkHttpBridge(
                store = store,
                config = config,
                keyResolver = keyResolver,
                engine = engine,
            )
        }

        @JvmStatic
        @JvmOverloads
        fun install(
            builder: OkHttpClient.Builder,
            context: Context,
            config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
        ): RetrostashOkHttpBridge {
            val bridge = create(context, config)
            bridge.install(builder)
            return bridge
        }

        @JvmStatic
        @JvmOverloads
        fun clear(
            context: Context,
            config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
        ) {
            AndroidRetrostashStore.clear(context, config)
        }

        fun from(client: OkHttpClient): RetrostashOkHttpBridge? {
            return client.interceptors
                .asSequence()
                .filterIsInstance<RetrostashOkHttpHandleInterceptor>()
                .map { it.bridge }
                .firstOrNull()
        }
    }
}
