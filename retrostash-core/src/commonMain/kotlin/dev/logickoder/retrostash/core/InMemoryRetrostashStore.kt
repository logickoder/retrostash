package dev.logickoder.retrostash.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class InMemoryRetrostashStore : RetrostashStore {
    private val entries = linkedMapOf<String, Entry>()
    private val mutex = Mutex()

    override suspend fun get(key: String): ByteArray? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        if (entry.isExpired()) {
            entries.remove(key)
            return@withLock null
        }
        entry.payload
    }

    override suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long) = mutex.withLock {
        entries[key] = Entry(
            payload = payload,
            createdAt = TimeSource.Monotonic.markNow(),
            maxAgeMs = maxAgeMs.coerceAtLeast(0L),
        )
    }

    override suspend fun invalidate(template: String) = mutex.withLock {
        if (entries.remove(template) != null) {
            return@withLock
        }
        val marker = "|$template|"
        val keysToRemove = entries.keys.filter { key -> key.contains(marker) }
        keysToRemove.forEach(entries::remove)
    }

    override suspend fun clear() = mutex.withLock {
        entries.clear()
    }

    private class Entry(
        val payload: ByteArray,
        val createdAt: TimeSource.Monotonic.ValueTimeMark,
        val maxAgeMs: Long,
    ) {
        fun isExpired(): Boolean {
            if (maxAgeMs <= 0L) return false
            return createdAt.elapsedNow() > maxAgeMs.milliseconds
        }
    }
}