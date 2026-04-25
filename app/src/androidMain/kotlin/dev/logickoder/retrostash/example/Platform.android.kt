package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.example.domain.OkHttpDemoEngine
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

actual val platformName: String = "Android"

actual fun createPlatformKtorEngine(): HttpClientEngineFactory<*> = OkHttp

actual val isOkHttpSupported: Boolean = true

actual fun createOkHttpDemoEngine(
    store: RetrostashStore,
    onLog: (String) -> Unit,
): DemoEngine? {
    val bridge = RetrostashOkHttpBridge(
        store = store,
        config = RetrostashOkHttpConfig(logger = onLog),
    )
    val client = OkHttpClient.Builder().also(bridge::install).build()
    return OkHttpDemoEngine(client = client, onLog = onLog)
}

