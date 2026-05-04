package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import dev.logickoder.retrostash.core.QueryMetadata
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrostashOkHttpBridgeTest {

    @Test
    fun install_adds_handle_and_policy_interceptors() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        val builder = OkHttpClient.Builder()

        bridge.install(builder)
        val client = builder.build()

        val found = RetrostashOkHttpBridge.from(client)
        assertNotNull(found)
        assertEquals(bridge, found)
        assertTrue(client.interceptors.any { it is RetrostashOkHttpHandleInterceptor })
        assertTrue(client.interceptors.any { it is RetrostashOkHttpInterceptor })
    }

    @Test
    fun invalidateQueryKey_returns_false_for_blank_key() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        val result = bridge.invalidateQueryKey("  ")
        assertEquals(false, result)
    }

    @Test
    fun invalidateTag_clears_entries_with_matching_tag() {
        val store = InMemoryRetrostashStore()
        val bridge = RetrostashOkHttpBridge(store = store)
        val metadata = QueryMetadata(
            scopeName = "ArticleApi",
            template = "native_article/{guid}",
            bindings = mapOf("guid" to "abc"),
            tagTemplates = listOf("article:{guid}"),
        )
        runBlocking {
            bridge.engine.persistQueryResult(metadata, "payload".encodeToByteArray(), 60_000L)
            assertNotNull(bridge.engine.resolveFromCache(metadata))
        }

        val scheduled = bridge.invalidateTag("article:abc")

        assertEquals(true, scheduled)
        runBlocking {
            assertNull(bridge.engine.resolveFromCache(metadata))
        }
    }

    @Test
    fun invalidateTag_blank_returns_false() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        assertEquals(false, bridge.invalidateTag("   "))
    }

    @Test
    fun invalidateTags_vararg_clears_each_provided_tag() {
        val store = InMemoryRetrostashStore()
        val bridge = RetrostashOkHttpBridge(store = store)
        val article = QueryMetadata(
            scopeName = "ArticleApi",
            template = "native_article/{guid}",
            bindings = mapOf("guid" to "abc"),
            tagTemplates = listOf("article:{guid}"),
        )
        val like = QueryMetadata(
            scopeName = "LikeApi",
            template = "like_status/{contentUri}",
            bindings = mapOf("contentUri" to "/path/to/article"),
            tagTemplates = listOf("article:{contentUri}"),
        )
        runBlocking {
            bridge.engine.persistQueryResult(article, "a".encodeToByteArray(), 60_000L)
            bridge.engine.persistQueryResult(like, "l".encodeToByteArray(), 60_000L)
        }

        val scheduled = bridge.invalidateTags(
            "article:abc",
            "article:/path/to/article",
        )

        assertEquals(true, scheduled)
        runBlocking {
            assertNull(bridge.engine.resolveFromCache(article))
            assertNull(bridge.engine.resolveFromCache(like))
        }
    }

    @Test
    fun invalidateTags_returns_false_when_all_blank() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        assertEquals(false, bridge.invalidateTags("", "  "))
    }
}
