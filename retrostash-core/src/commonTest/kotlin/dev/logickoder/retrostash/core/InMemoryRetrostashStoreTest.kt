package dev.logickoder.retrostash.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun invalidate_tag_clears_only_matching_entries() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("article-key", "article".encodeToByteArray(), 60_000L, tags = setOf("article:7"))
        store.put("like-key", "like".encodeToByteArray(), 60_000L, tags = setOf("article:7", "like:42"))
        store.put("untagged-key", "other".encodeToByteArray(), 60_000L)

        store.invalidateTag("article:7")

        assertNull(store.get("article-key"))
        assertNull(store.get("like-key"))
        assertNotNull(store.get("untagged-key"))
    }

    @Test
    fun invalidate_blank_tag_is_noop() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("k", "v".encodeToByteArray(), 60_000L, tags = setOf("article:7"))

        store.invalidateTag("")
        store.invalidateTag("   ")

        assertNotNull(store.get("k"))
    }

    @Test
    fun patch_preserves_tags_when_null_passed() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("k", "v1".encodeToByteArray(), 60_000L, tags = setOf("article:7"))

        store.patch("k", "v2".encodeToByteArray(), maxAgeMs = null, tags = null)

        assertContentEquals("v2".encodeToByteArray(), store.get("k"))
        // Tag still present — invalidateTag still clears
        store.invalidateTag("article:7")
        assertNull(store.get("k"))
    }

    @Test
    fun patch_replaces_tags_when_non_null_set_passed() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("k", "v1".encodeToByteArray(), 60_000L, tags = setOf("article:7"))

        store.patch("k", "v2".encodeToByteArray(), tags = setOf("user:42"))

        store.invalidateTag("article:7")
        assertNotNull(store.get("k"))  // article:7 no longer matches
        store.invalidateTag("user:42")
        assertNull(store.get("k"))
    }

    @Test
    fun patch_clears_tags_when_empty_set_passed() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("k", "v1".encodeToByteArray(), 60_000L, tags = setOf("article:7"))

        store.patch("k", "v2".encodeToByteArray(), tags = emptySet())

        store.invalidateTag("article:7")
        assertNotNull(store.get("k"))  // tag was explicitly cleared
    }

    @Test
    fun patch_creates_entry_when_no_existing_with_default_metadata() = runTest {
        val store = InMemoryRetrostashStore()
        // No put first
        store.patch("k", "v".encodeToByteArray(), maxAgeMs = null, tags = null)
        assertContentEquals("v".encodeToByteArray(), store.get("k"))
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
