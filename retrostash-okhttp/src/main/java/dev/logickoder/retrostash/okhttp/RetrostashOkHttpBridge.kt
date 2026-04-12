package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import dev.logickoder.retrostash.core.RetrostashStore
import okhttp3.OkHttpClient

class RetrostashOkHttpBridge(
    val store: RetrostashStore,
    val engine: RetrostashEngine = RetrostashEngine(
        store = store,
        keyResolver = CoreKeyResolver(),
    ),
) {
    fun install(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.addInterceptor(RetrostashOkHttpInterceptor(engine))
        return builder
    }
}
