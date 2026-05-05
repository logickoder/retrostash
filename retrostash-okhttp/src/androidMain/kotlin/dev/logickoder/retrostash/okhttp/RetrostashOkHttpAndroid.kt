package dev.logickoder.retrostash.okhttp

import android.content.Context
import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import okhttp3.OkHttpClient

/**
 * Android convenience factories for [RetrostashOkHttpBridge].
 *
 * Combines an [AndroidRetrostashStore] (Context-backed disk + SharedPreferences index) with the
 * bridge so callers don't need to wire the store manually. JVM-only consumers should construct
 * [RetrostashOkHttpBridge] directly.
 */
object RetrostashOkHttpAndroid {

    /** Creates a [RetrostashOkHttpBridge] backed by an [AndroidRetrostashStore]. */
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

    /**
     * Creates a bridge and installs its interceptors onto [builder]. Returns the bridge so
     * callers can keep a reference for direct invalidation later.
     *
     * **Caching strategy:** if [builder] also has `cache(...)` set, you have two cache layers.
     * Retrostash invalidation does **not** evict OkHttp HTTP cache entries — see
     * [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy) for the
     * recommended configuration before shipping.
     */
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

    /**
     * Clears the entire on-disk cache for the namespace defined by [config]. Suitable for
     * "logout" flows where every cached entry must go. Cheaper than instantiating a bridge just
     * to call `store.clear()`.
     */
    @JvmStatic
    @JvmOverloads
    fun clear(
        context: Context,
        config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
    ) {
        AndroidRetrostashStore.clear(context, config)
    }
}
