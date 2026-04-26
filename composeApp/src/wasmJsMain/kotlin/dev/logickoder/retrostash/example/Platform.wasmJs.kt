package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual object Platform {
    actual val name: String = "Web · wasmJs"

    actual val ktorEngine: HttpClientEngineFactory<*> = Js

    actual fun createOkHttpEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? = null

    actual fun createRetrofitEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? = null
}
