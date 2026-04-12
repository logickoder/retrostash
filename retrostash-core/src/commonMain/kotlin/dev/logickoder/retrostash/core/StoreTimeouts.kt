package dev.logickoder.retrostash.core

import kotlinx.coroutines.withTimeoutOrNull

suspend fun <T> withStoreTimeoutOrNull(timeoutMs: Long, block: suspend () -> T): T? {
    if (timeoutMs <= 0L) {
        return block()
    }
    return withTimeoutOrNull(timeoutMs) { block() }
}
