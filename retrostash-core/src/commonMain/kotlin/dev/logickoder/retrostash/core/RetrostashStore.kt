package dev.logickoder.retrostash.core

interface RetrostashStore {
    suspend fun get(key: String): ByteArray?

    suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long)

    suspend fun invalidate(template: String)

    suspend fun clear()
}
