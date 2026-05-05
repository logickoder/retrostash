package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RetrostashKtorCacheTest {

    private fun runtime() = RetrostashKtorRuntime.create(InMemoryRetrostashStore())

    @Test
    fun peekQuery_returns_null_when_not_cached() = runTest {
        val cache = runtime().cache
        assertNull(cache.peekQuery("UserApi", "users/{id}", mapOf("id" to "42")))
    }

    @Test
    fun update_then_peek_round_trips_payload() = runTest {
        val cache = runtime().cache
        val payload = "{\"id\":42}".encodeToByteArray()

        val key = cache.updateQuery(
            scopeName = "UserApi",
            template = "users/{id}",
            bindings = mapOf("id" to "42"),
            payload = payload,
            maxAgeMs = 60_000L,
        )
        assertNotNull(key)

        val read = cache.peekQuery("UserApi", "users/{id}", mapOf("id" to "42"))
        assertContentEquals(payload, read)
    }

    @Test
    fun updateQuery_returns_null_when_placeholder_unresolved() = runTest {
        val cache = runtime().cache
        assertNull(
            cache.updateQuery(
                "UserApi",
                "users/{id}",
                emptyMap(),
                "x".encodeToByteArray(),
            )
        )
    }

    @Test
    fun updateQuery_persists_tags_so_invalidateTag_clears_entry() = runTest {
        val cache = runtime().cache
        cache.updateQuery(
            scopeName = "ArticleApi",
            template = "articles/{guid}",
            bindings = mapOf("guid" to "abc"),
            payload = "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{guid}"),
        )
        assertNotNull(cache.peekQuery("ArticleApi", "articles/{guid}", mapOf("guid" to "abc")))

        cache.invalidateTag("article:abc")

        assertNull(cache.peekQuery("ArticleApi", "articles/{guid}", mapOf("guid" to "abc")))
    }

    @Test
    fun invalidateQuery_clears_matching_entry() = runTest {
        val cache = runtime().cache
        cache.updateQuery(
            "UserApi",
            "users/{id}",
            mapOf("id" to "7"),
            "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
        )

        val resolved = cache.invalidateQuery("UserApi", "users/{id}", mapOf("id" to "7"))

        assertNotNull(resolved)
        assertNull(cache.peekQuery("UserApi", "users/{id}", mapOf("id" to "7")))
    }

    @Test
    fun invalidateTags_clears_each_provided_tag() = runTest {
        val cache = runtime().cache
        cache.updateQuery(
            "ArticleApi",
            "articles/{guid}",
            mapOf("guid" to "abc"),
            "a".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{guid}"),
        )
        cache.updateQuery(
            "LikeApi",
            "likes/{contentUri}",
            mapOf("contentUri" to "/x"),
            "l".encodeToByteArray(),
            maxAgeMs = 60_000L,
            tags = listOf("article:{contentUri}"),
        )

        cache.invalidateTags(listOf("article:abc", "article:/x"))

        assertNull(cache.peekQuery("ArticleApi", "articles/{guid}", mapOf("guid" to "abc")))
        assertNull(cache.peekQuery("LikeApi", "likes/{contentUri}", mapOf("contentUri" to "/x")))
    }

    @Test
    fun clearAll_empties_store() = runTest {
        val cache = runtime().cache
        cache.updateQuery(
            "UserApi",
            "users/{id}",
            mapOf("id" to "1"),
            "p".encodeToByteArray(),
            maxAgeMs = 60_000L,
        )

        cache.clearAll()

        assertNull(cache.peekQuery("UserApi", "users/{id}", mapOf("id" to "1")))
    }

    @Test
    fun bodyBytes_fallback_resolves_placeholder_for_peek() = runTest {
        val cache = runtime().cache
        cache.updateQuery(
            scopeName = "PostApi",
            template = "posts/{postId}",
            bindings = emptyMap(),
            bodyBytes = "{\"postId\":1337}".encodeToByteArray(),
            payload = "body".encodeToByteArray(),
            maxAgeMs = 60_000L,
        )

        val read = cache.peekQuery(
            scopeName = "PostApi",
            template = "posts/{postId}",
            bindings = emptyMap(),
            bodyBytes = "{\"postId\":1337}".encodeToByteArray(),
        )
        assertEquals("body", read?.decodeToString())
    }
}
