package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.engine.HttpClientEngineFactory

expect object Platform : IPlatform

private val MONO_START = kotlin.time.TimeSource.Monotonic.markNow()

interface IPlatform {
    val name: String

    val ktorEngine: HttpClientEngineFactory<*>

    fun createOkHttpEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? = null

    fun createRetrofitEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? = null

    fun nowMs(): Long = MONO_START.elapsedNow().inWholeMilliseconds
}
