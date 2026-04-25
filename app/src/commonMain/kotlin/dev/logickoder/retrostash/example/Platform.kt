package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory

expect val platformName: String

expect fun createPlatformKtorEngine(): HttpClientEngineFactory<*>

expect val isOkHttpSupported: Boolean

expect fun createOkHttpDemoEngine(
    store: RetrostashStore,
    onLog: (String) -> Unit,
): DemoEngine?

private val MONO_START = kotlin.time.TimeSource.Monotonic.markNow()

fun nowMs(): Long = MONO_START.elapsedNow().inWholeMilliseconds
