package dev.logickoder.retrostash.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertTrue

class InMemoryRetrostashStoreTest {

    @Test
    fun concurrent_put_get_invalidate_does_not_throw() = runTest {
        val store = InMemoryRetrostashStore()
        val workers = 32
        val opsPerWorker = 200

        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(workers) { w ->
                    launch {
                        repeat(opsPerWorker) { i ->
                            val key = "scope|users/${i % 16}|tenant=$w"
                            store.put(key, byteArrayOf(w.toByte(), i.toByte()), maxAgeMs = 60_000L)
                            store.get(key)
                            if (i % 8 == 0) store.invalidate("users/${i % 16}")
                        }
                    }
                }
            }
        }

        store.clear()
        assertTrue(true, "completed without ConcurrentModificationException")
    }

    @Test
    fun parallel_get_after_put_returns_payload() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("k", "v".encodeToByteArray(), maxAgeMs = 60_000L)

        withContext(Dispatchers.Default) {
            val results = (1..50).map {
                async { store.get("k") }
            }.awaitAll()
            assertTrue(results.all { it != null && it.decodeToString() == "v" })
        }
    }
}
