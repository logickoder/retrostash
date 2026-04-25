package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual val platformName: String = "Web · wasmJs"

actual fun createPlatformKtorEngine(): HttpClientEngineFactory<*> = Js

actual val isOkHttpSupported: Boolean = false

actual fun createOkHttpDemoEngine(
    store: RetrostashStore,
    onLog: (String) -> Unit,
): DemoEngine? = null

