package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import okhttp3.OkHttpClient

actual val platformName: String = "Desktop · ${System.getProperty("os.name")}"

actual fun createPlatformKtorEngine(): HttpClientEngineFactory<*> = CIO

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

