package dev.logickoder.retrostash.okhttp

import android.content.Context
import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import okhttp3.OkHttpClient

object RetrostashOkHttpAndroid {

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
}
