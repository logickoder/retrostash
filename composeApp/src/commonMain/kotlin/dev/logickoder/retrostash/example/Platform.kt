package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.engine.HttpClientEngineFactory

private val MONO_START = kotlin.time.TimeSource.Monotonic.markNow()

expect object Platform {
    val name: String

    val ktorEngine: HttpClientEngineFactory<*>

    fun createOkHttpEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine?

    fun createRetrofitEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine?
}

fun Platform.nowMs(): Long = MONO_START.elapsedNow().inWholeMilliseconds
