package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrostashOkHttpCacheTest {

    private fun bridge() = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())

    @Test
    fun peekQuery_returns_null_when_not_cached() {
        val cache = bridge().cache
        val result = cache.peekQuery(
            apiClass = UserApi::class.java,
            template = "users/{id}",
            bindings = mapOf("id" to "42"),
        )
        assertNull(result)
    }

    @Test
    fun update_then_peek_round_trips_payload() {
        val cache = bridge().cache
        val payload = "{\"id\":42}".encodeToByteArray()

        val key = cache.updateQuery(
            apiClass = UserApi::class.java,
            template = "users/{id}",
            bindings = mapOf("id" to "42"),
            payload = payload,
            maxAgeMs = 60_000L,
        )
        assertNotNull(key)

        val read = cache.peekQuery(UserApi::class.java, "users/{id}", mapOf("id" to "42"))
        assertNotNull(read)
        assertArrayEquals(payload, read)
    }

    @Test
    fun update_wraps_in_envelope_with_supplied_content_type() {
        val store = InMemoryRetrostashStore()
        val bridge = RetrostashOkHttpBridge(store = store)
        val cache = bridge.cache
        val payload = "raw-text".encodeToByteArray()

        val key = cache.updateQuery(
            apiClass = UserApi::class.java,
            template = "users/{id}",
            bindings = mapOf("id" to "1"),
            payload = payload,
            contentType = "text/plain",
            maxAgeMs = 60_000L,
        ) ?: error("key resolution should succeed")

        val raw = kotlinx.coroutines.runBlocking { store.get(key) } ?: error("entry missing")
        val envelope = CachedHttpEnvelopeCodec.decode(raw)
        assertNotNull(envelope)
        assertEquals(200, envelope!!.statusCode)
        assertEquals("text/plain", envelope.contentType)
        assertArrayEquals(payload, envelope.payload)
    }

    @Test
    fun updateQuery_returns_null_when_placeholder_unresolved() {
        val cache = bridge().cache
        val key = cache.updateQuery(
            apiClass = UserApi::class.java,
            template = "users/{id}",
            bindings = emptyMap(),
            payload = "x".encodeToByteArray(),
        )
        assertNull(key)
    }

    @Test
    fun updateQuery_persists_tags_so_invalidateTag_clears_entry() {
        val cache = bridge().cache
        cache.updateQuery(
            apiClass = ArticleApi::class.java,
            template = "articles/{guid}",
            bindings = mapOf("guid" to "abc"),
            payload = "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{guid}"),
        )

        assertNotNull(cache.peekQuery(ArticleApi::class.java, "articles/{guid}", mapOf("guid" to "abc")))

        val cleared = cache.invalidateTag("article:abc")
        assertTrue(cleared)
        assertNull(cache.peekQuery(ArticleApi::class.java, "articles/{guid}", mapOf("guid" to "abc")))
    }

    @Test
    fun invalidateQuery_clears_matching_entry() {
        val cache = bridge().cache
        cache.updateQuery(
            UserApi::class.java,
            "users/{id}",
            mapOf("id" to "7"),
            "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
        )

        val resolved = cache.invalidateQuery(UserApi::class.java, "users/{id}", mapOf("id" to "7"))

        assertNotNull(resolved)
        assertNull(cache.peekQuery(UserApi::class.java, "users/{id}", mapOf("id" to "7")))
    }

    @Test
    fun invalidateQueryKey_blank_returns_false() {
        val cache = bridge().cache
        assertFalse(cache.invalidateQueryKey("   "))
    }

    @Test
    fun invalidateTags_vararg_clears_each_provided_tag() {
        val cache = bridge().cache
        cache.updateQuery(
            ArticleApi::class.java,
            "articles/{guid}",
            mapOf("guid" to "abc"),
            "a".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{guid}"),
        )
        cache.updateQuery(
            LikeApi::class.java,
            "likes/{contentUri}",
            mapOf("contentUri" to "/x"),
            "l".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{contentUri}"),
        )

        assertTrue(cache.invalidateTags("article:abc", "article:/x"))

        assertNull(cache.peekQuery(ArticleApi::class.java, "articles/{guid}", mapOf("guid" to "abc")))
        assertNull(cache.peekQuery(LikeApi::class.java, "likes/{contentUri}", mapOf("contentUri" to "/x")))
    }

    @Test
    fun invalidateTags_returns_false_when_all_blank() {
        val cache = bridge().cache
        assertFalse(cache.invalidateTags("", "  "))
    }

    @Test
    fun clearAll_empties_store() {
        val store = InMemoryRetrostashStore()
        val bridge = RetrostashOkHttpBridge(store = store)
        val cache = bridge.cache
        cache.updateQuery(
            UserApi::class.java,
            "users/{id}",
            mapOf("id" to "1"),
            "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
        )

        cache.clearAll()

        assertNull(cache.peekQuery(UserApi::class.java, "users/{id}", mapOf("id" to "1")))
    }

    private interface UserApi
    private interface ArticleApi
    private interface LikeApi
}
