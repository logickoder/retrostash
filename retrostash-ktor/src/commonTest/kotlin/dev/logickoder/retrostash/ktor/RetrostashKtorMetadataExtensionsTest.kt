package dev.logickoder.retrostash.ktor

import io.ktor.client.request.HttpRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RetrostashKtorMetadataExtensionsTest {

    @Test
    fun retrostashQuery_sets_metadata() {
        val builder = HttpRequestBuilder()
        builder.retrostashQuery(
            scopeName = "UserApi",
            template = "users/{id}",
            bindings = mapOf("id" to "42"),
            maxAgeMs = 30_000L,
        )

        val metadata = builder.attributes.getOrNull(RetrostashMetadataKey)
        assertNotNull(metadata)
        assertEquals("UserApi", metadata.scopeName)
        assertEquals("users/{id}", metadata.queryTemplate)
        assertEquals("42", metadata.bindings["id"])
        assertEquals(30_000L, metadata.maxAgeMs)
    }

    @Test
    fun retrostashMutate_merges_with_existing_query_metadata() {
        val builder = HttpRequestBuilder()
        builder
            .retrostashQuery(
                scopeName = "UserApi",
                template = "users/{id}",
                bindings = mapOf("id" to "42"),
            )
            .retrostashMutate(
                scopeName = "UserApi",
                invalidateTemplates = listOf("users/{id}", "users/list"),
                bindings = mapOf("tenant" to "acme"),
            )

        val metadata = builder.attributes.getOrNull(RetrostashMetadataKey)
        assertNotNull(metadata)
        assertEquals("users/{id}", metadata.queryTemplate)
        assertEquals("42", metadata.bindings["id"])
        assertEquals("acme", metadata.bindings["tenant"])
        assertEquals(2, metadata.invalidateTemplates.size)
        assertTrue(metadata.invalidateTemplates.contains("users/{id}"))
        assertTrue(metadata.invalidateTemplates.contains("users/list"))
    }
}
