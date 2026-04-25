package dev.logickoder.retrostash.example

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.UIKit.UIDevice

actual object Platform : IPlatform {
    override val name: String =
        "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"

    override val ktorEngine: HttpClientEngineFactory<*> = Darwin
}