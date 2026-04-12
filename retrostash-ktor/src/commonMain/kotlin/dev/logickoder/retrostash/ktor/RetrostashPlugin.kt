package dev.logickoder.retrostash.ktor

import io.ktor.client.plugins.api.createClientPlugin

val RetrostashPlugin = createClientPlugin("Retrostash", ::RetrostashConfig) {
}
