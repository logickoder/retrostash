package dev.logickoder.retrostash.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CoreKeyResolverTest {

    @Test
    fun resolves_placeholders_from_bindings() {
        val resolver = CoreKeyResolver()
        val key = resolver.resolve(
            QueryMetadata(
                scopeName = "UserApi",
                template = "users/{id}?tenant={tenant}",
                bindings = mapOf("id" to "42", "tenant" to "acme"),
            )
        )

        assertNotNull(key)
        assertEquals(true, key.startsWith("UserApi|users/42?tenant=acme|"))
    }

    @Test
    fun resolves_missing_placeholders_from_json_body() {
        val resolver = CoreKeyResolver()
        val key = resolver.resolve(
            QueryMetadata(
                scopeName = "FeedApi",
                template = "posts/{postId}?tenant={tenant}",
                bindings = mapOf("tenant" to "acme"),
                bodyBytes = "{\"postId\":1337}".encodeToByteArray(),
            )
        )

        assertNotNull(key)
        assertEquals(true, key.startsWith("FeedApi|posts/1337?tenant=acme|"))
    }

    @Test
    fun returns_null_when_placeholder_unresolved() {
        val resolver = CoreKeyResolver()
        val key = resolver.resolve(
            QueryMetadata(
                scopeName = "UserApi",
                template = "users/{id}",
                bindings = emptyMap(),
                bodyBytes = "{\"tenant\":\"acme\"}".encodeToByteArray(),
            )
        )

        assertNull(key)
    }

    @Test
    fun resolves_tag_templates_against_bindings_and_body() {
        val resolver = CoreKeyResolver()
        val tags = resolver.resolveTags(
            QueryMetadata(
                scopeName = "ArticleApi",
                template = "native_article/{guid}",
                bindings = mapOf("guid" to "abc"),
                bodyBytes = "{\"conceptId\":\"concept-7\"}".encodeToByteArray(),
                tagTemplates = listOf("article:{guid}", "article:{conceptId}"),
            )
        )

        assertEquals(listOf("article:abc", "article:concept-7"), tags)
    }

    @Test
    fun drops_tag_templates_with_unresolved_placeholders() {
        val resolver = CoreKeyResolver()
        val tags = resolver.resolveTags(
            QueryMetadata(
                scopeName = "ArticleApi",
                template = "native_article/{guid}",
                bindings = mapOf("guid" to "abc"),
                tagTemplates = listOf("article:{guid}", "article:{missing}"),
            )
        )

        assertEquals(listOf("article:abc"), tags)
    }

    @Test
    fun returns_empty_when_no_tag_templates() {
        val resolver = CoreKeyResolver()
        val tags = resolver.resolveTags(
            QueryMetadata(
                scopeName = "ArticleApi",
                template = "native_article/{guid}",
                bindings = mapOf("guid" to "abc"),
            )
        )

        assertEquals(emptyList(), tags)
    }

    @Test
    fun reads_nested_json_value() {
        val value = Utf8JsonLookup.findFirstPrimitiveByKey(
            payload = "{\"meta\":{\"user\":{\"id\":\"abc\"}}}".encodeToByteArray(),
            key = "id",
        )

        assertEquals("abc", value)
    }
}
