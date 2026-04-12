package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.CoreKeyResolver
import dev.logickoder.retrostash.core.RetrostashEngine
import io.ktor.client.plugins.api.createClientPlugin

val RetrostashPlugin = createClientPlugin("Retrostash", ::RetrostashConfig) {
	val configuredStore = pluginConfig.store ?: return@createClientPlugin
	val engine = RetrostashEngine(
		store = configuredStore,
		keyResolver = CoreKeyResolver(),
		timeoutMs = pluginConfig.timeoutMs,
	)

	// Engine initialization is intentionally separated from request hooks.
	// Interception behavior will use request attributes in the next step.
	engine
}
