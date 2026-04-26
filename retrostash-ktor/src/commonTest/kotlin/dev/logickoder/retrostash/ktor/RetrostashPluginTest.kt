package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RetrostashPluginTest {

    private fun mockClient(
        store: InMemoryRetrostashStore,
        status: HttpStatusCode,
        body: String,
    ) = HttpClient(MockEngine { _ ->
        respond(
            content = ByteReadChannel(body.encodeToByteArray()),
            status = status,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )
    }) {
        install(RetrostashPlugin) {
            this.store = store
        }
    }

    @Test
    fun does_not_invalidate_on_5xx_response() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("FeedApi|feed/7|", "cached".encodeToByteArray(), maxAgeMs = 60_000L)

        val client = mockClient(store, HttpStatusCode.InternalServerError, "boom")
        client.post("https://example.com/feed/7") {
            retrostashMutate(
                scopeName = "FeedApi",
                invalidateTemplates = listOf("feed/7"),
                bindings = mapOf("id" to "7"),
            )
        }

        assertContentEquals("cached".encodeToByteArray(), store.get("FeedApi|feed/7|"))
        client.close()
    }

    @Test
    fun invalidates_on_2xx_response() = runTest {
        val store = InMemoryRetrostashStore()
        store.put("FeedApi|feed/7|", "cached".encodeToByteArray(), maxAgeMs = 60_000L)

        val client = mockClient(store, HttpStatusCode.OK, "ok")
        client.post("https://example.com/feed/7") {
            retrostashMutate(
                scopeName = "FeedApi",
                invalidateTemplates = listOf("feed/7"),
                bindings = mapOf("id" to "7"),
            )
        }

        assertNull(store.get("FeedApi|feed/7|"))
        client.close()
    }

    @Test
    fun persists_query_response_on_2xx() = runTest {
        val store = InMemoryRetrostashStore()
        val client = mockClient(store, HttpStatusCode.OK, """{"ok":true}""")

        client.get("https://example.com/feed/7") {
            retrostashQuery(
                scopeName = "FeedApi",
                template = "feed/{id}",
                bindings = mapOf("id" to "7"),
                maxAgeMs = 60_000L,
            )
        }

        val resolver = dev.logickoder.retrostash.core.CoreKeyResolver()
        val key = resolver.resolve(
            dev.logickoder.retrostash.core.QueryMetadata(
                scopeName = "FeedApi",
                template = "feed/{id}",
                bindings = mapOf("id" to "7"),
            )
        )
        assertNotNull(key)
        assertContentEquals("""{"ok":true}""".encodeToByteArray(), store.get(key))
        client.close()
    }
}
