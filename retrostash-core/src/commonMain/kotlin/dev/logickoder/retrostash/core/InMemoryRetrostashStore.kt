package dev.logickoder.retrostash.core

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class InMemoryRetrostashStore : RetrostashStore {
    private val entries = linkedMapOf<String, Entry>()

    override suspend fun get(key: String): ByteArray? {
        val entry = entries[key] ?: return null
        if (entry.isExpired()) {
            entries.remove(key)
            return null
        }
        return entry.payload
    }

    override suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long) {
        entries[key] = Entry(
            payload = payload,
            createdAt = TimeSource.Monotonic.markNow(),
            maxAgeMs = maxAgeMs.coerceAtLeast(0L),
        )
    }

    override suspend fun invalidate(template: String) {
        val marker = "|$template|"
        val keysToRemove = entries.keys.filter { key -> key.contains(marker) }
        keysToRemove.forEach(entries::remove)
    }

    override suspend fun clear() {
        entries.clear()
    }

    private data class Entry(
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