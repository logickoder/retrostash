package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.RetrostashStore

class RetrostashConfig {
    lateinit var store: RetrostashStore
    var timeoutMs: Long = 250L
}
