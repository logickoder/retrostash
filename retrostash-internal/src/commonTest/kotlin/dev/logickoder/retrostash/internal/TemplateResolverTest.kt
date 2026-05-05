package dev.logickoder.retrostash.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(RetrostashInternalApi::class)
class TemplateResolverTest {

    @Test
    fun returns_template_unchanged_when_no_placeholders() {
        val resolved = TemplateResolver.resolve(
            template = "users",
            bindings = emptyMap(),
            bodyBytes = null,
        )
        assertEquals("users", resolved)
    }

    @Test
    fun resolves_placeholders_from_bindings() {
        val resolved = TemplateResolver.resolve(
            template = "users/{id}?tenant={tenant}",
            bindings = mapOf("id" to "42", "tenant" to "acme"),
            bodyBytes = null,
        )
        assertEquals("users/42?tenant=acme", resolved)
    }

    @Test
    fun falls_back_to_json_body_lookup() {
        val resolved = TemplateResolver.resolve(
            template = "posts/{postId}",
            bindings = emptyMap(),
            bodyBytes = "{\"postId\":1337}".encodeToByteArray(),
        )
        assertEquals("posts/1337", resolved)
    }

    @Test
    fun returns_null_when_placeholder_unresolved() {
        val resolved = TemplateResolver.resolve(
            template = "users/{id}",
            bindings = emptyMap(),
            bodyBytes = "{\"tenant\":\"acme\"}".encodeToByteArray(),
        )
        assertNull(resolved)
    }
}
