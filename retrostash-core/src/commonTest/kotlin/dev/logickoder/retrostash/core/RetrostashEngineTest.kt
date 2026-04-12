package dev.logickoder.retrostash.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class RetrostashEngineTest {

    @Test
    fun stores_and_reads_cached_payload() {
        val store = InMemoryRetrostashStore()
        val engine = RetrostashEngine(store = store)
        val metadata = QueryMetadata(
            scopeName = "UserApi",
            template = "users/{id}",
            bindings = mapOf("id" to "42"),
        )

        kotlinx.coroutines.test.runTest {
            engine.persistQueryResult(metadata, "payload".encodeToByteArray(), maxAgeMs = 60_000)
            val cached = engine.resolveFromCache(metadata)
            assertContentEquals("payload".encodeToByteArray(), cached)
        }
    }

    @Test
    fun invalidates_matching_templates() {
        val store = InMemoryRetrostashStore()
        val engine = RetrostashEngine(store = store)
        val metadata = QueryMetadata(
            scopeName = "UserApi",
            template = "users/{id}",
            bindings = mapOf("id" to "7"),
        )

        kotlinx.coroutines.test.runTest {
            engine.persistQueryResult(metadata, "payload".encodeToByteArray(), maxAgeMs = 60_000)
            engine.invalidateTemplates(listOf("users/7"))
            val cached = engine.resolveFromCache(metadata)
            assertNull(cached)
        }
    }
}
