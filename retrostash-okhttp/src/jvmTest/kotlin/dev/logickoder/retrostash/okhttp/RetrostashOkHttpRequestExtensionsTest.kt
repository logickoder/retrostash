package dev.logickoder.retrostash.okhttp

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrostashOkHttpRequestExtensionsTest {

    @Test
    fun retrostashQuery_attaches_expected_metadata() {
        val request = Request.Builder()
            .url("https://example.com/users/42")
            .retrostashQuery(
                scopeName = "UserApi",
                template = "users/{id}",
                bindings = mapOf("id" to "42"),
                maxAgeMs = 60_000L,
            )
            .build()

        val metadata = request.tag(OkHttpRetrostashMetadata::class.java)
        assertNotNull(metadata)
        assertEquals("UserApi", metadata?.scopeName)
        assertEquals("users/{id}", metadata?.queryTemplate)
        assertEquals("42", metadata?.bindings?.get("id"))
        assertEquals(60_000L, metadata?.maxAgeMs)
    }

    @Test
    fun repeated_calls_merge_bindings_and_invalidations() {
        val request = Request.Builder()
            .url("https://example.com/users/42")
            .retrostashQuery(
                scopeName = "UserApi",
                template = "users/{id}",
                bindings = mapOf("id" to "42"),
                maxAgeMs = 60_000L,
            )
            .retrostashMutate(
                scopeName = "UserApi",
                invalidateTemplates = listOf("users/{id}", "users/list"),
                bindings = mapOf("tenant" to "acme"),
            )
            .build()

        val metadata = request.tag(OkHttpRetrostashMetadata::class.java)
        assertNotNull(metadata)
        assertEquals("users/{id}", metadata?.queryTemplate)
        assertEquals("42", metadata?.bindings?.get("id"))
        assertEquals("acme", metadata?.bindings?.get("tenant"))
        assertEquals(2, metadata?.invalidateTemplates?.size)
        assertEquals(true, metadata?.invalidateTemplates?.contains("users/{id}"))
        assertEquals(true, metadata?.invalidateTemplates?.contains("users/list"))
    }
}
