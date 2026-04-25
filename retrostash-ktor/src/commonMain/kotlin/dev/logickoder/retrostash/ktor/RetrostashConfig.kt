package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.RetrostashStore

class RetrostashConfig {
    var store: RetrostashStore? = null
    var timeoutMs: Long = 250L
    var logger: ((String) -> Unit)? = null
}
