package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.RetrostashStore

/**
 * Configuration for [RetrostashPlugin]. Set within `install(RetrostashPlugin) { ... }`:
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(RetrostashPlugin) {
 *         store = InMemoryRetrostashStore()
 *         timeoutMs = 250
 *         logger = { println(it) }
 *     }
 * }
 * ```
 *
 * @property store Backing cache. **Required** — if `null`, the plugin installs as a no-op.
 * @property timeoutMs Per-store-call deadline in milliseconds. Slow store calls fall through to
 * the network rather than blocking the request.
 * @property logger Optional event hook — receives `"retrostash: hit/miss/persisted/invalidated"`
 * lines. Useful for debugging cache behavior.
 */
class RetrostashConfig {
    var store: RetrostashStore? = null
    var timeoutMs: Long = 250L
    var logger: ((String) -> Unit)? = null
}
