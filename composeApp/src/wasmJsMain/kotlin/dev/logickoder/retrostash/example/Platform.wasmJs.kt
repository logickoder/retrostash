package dev.logickoder.retrostash.example

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual object Platform : IPlatform {
    override val name: String = "Web · wasmJs"

    override val ktorEngine: HttpClientEngineFactory<*> = Js
}