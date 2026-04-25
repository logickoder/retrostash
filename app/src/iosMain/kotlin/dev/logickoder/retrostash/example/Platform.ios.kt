package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.UIKit.UIDevice

actual val platformName: String =
    "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"

actual fun createPlatformKtorEngine(): HttpClientEngineFactory<*> = Darwin

actual val isOkHttpSupported: Boolean = false

actual fun createOkHttpDemoEngine(
    store: RetrostashStore,
    onLog: (String) -> Unit,
): DemoEngine? = null

