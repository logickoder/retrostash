package dev.logickoder.retrostash.ktor

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RetrostashKtorRuntimeTest {

    @Test
    fun resolve_and_persist_round_trip() = runTest {
        val runtime = RetrostashKtorRuntime.create(InMemoryRetrostashStore())
        val metadata = RetrostashKtorMetadata(
            scopeName = "FeedApi",
            queryTemplate = "feed/{id}",
            bindings = mapOf("id" to "7"),
            maxAgeMs = 60_000L,
        )

        runtime.persistQueryResult(metadata, "payload".encodeToByteArray())
        val cached = runtime.resolveFromCache(metadata)

        assertContentEquals("payload".encodeToByteArray(), cached)
    }

    @Test
    fun invalidate_clears_cached_value() = runTest {
        val runtime = RetrostashKtorRuntime.create(InMemoryRetrostashStore())
        val queryMetadata = RetrostashKtorMetadata(
            scopeName = "FeedApi",
            queryTemplate = "feed/{id}",
            bindings = mapOf("id" to "7"),
            maxAgeMs = 60_000L,
        )
        runtime.persistQueryResult(queryMetadata, "payload".encodeToByteArray())

        val mutateMetadata = RetrostashKtorMetadata(
            scopeName = "FeedApi",
            invalidateTemplates = listOf("feed/7"),
        )
        runtime.invalidate(mutateMetadata)

        val cached = runtime.resolveFromCache(queryMetadata)
        assertNull(cached)
    }

    @Test
    fun invalidate_clears_entries_via_resolved_tag_templates() = runTest {
        val runtime = RetrostashKtorRuntime.create(InMemoryRetrostashStore())
        val queryMetadata = RetrostashKtorMetadata(
            scopeName = "ArticleApi",
            queryTemplate = "article/{guid}",
            bindings = mapOf("guid" to "abc"),
            maxAgeMs = 60_000L,
            tagTemplates = listOf("article:{guid}"),
        )
        runtime.persistQueryResult(queryMetadata, "payload".encodeToByteArray())
        assertNotNull(runtime.resolveFromCache(queryMetadata))

        val mutateMetadata = RetrostashKtorMetadata(
            scopeName = "CommentApi",
            bindings = mapOf("conceptId" to "abc"),
            invalidateTagTemplates = listOf("article:{conceptId}"),
        )
        runtime.invalidate(mutateMetadata)

        assertNull(runtime.resolveFromCache(queryMetadata))
    }

    @Test
    fun invalidate_tags_imperative_call_clears_matching_entries() = runTest {
        val runtime = RetrostashKtorRuntime.create(InMemoryRetrostashStore())
        val queryMetadata = RetrostashKtorMetadata(
            scopeName = "ArticleApi",
            queryTemplate = "article/{guid}",
            bindings = mapOf("guid" to "abc"),
            maxAgeMs = 60_000L,
            tagTemplates = listOf("article:{guid}"),
        )
        runtime.persistQueryResult(queryMetadata, "payload".encodeToByteArray())

        runtime.invalidateTags(listOf("article:abc"))

        assertNull(runtime.resolveFromCache(queryMetadata))
    }

    @Test
    fun invalidate_resolves_placeholder_templates_against_bindings() = runTest {
        val runtime = RetrostashKtorRuntime.create(InMemoryRetrostashStore())
        val queryMetadata = RetrostashKtorMetadata(
            scopeName = "FeedApi",
            queryTemplate = "feed/{id}",
            bindings = mapOf("id" to "7"),
            maxAgeMs = 60_000L,
        )
        runtime.persistQueryResult(queryMetadata, "payload".encodeToByteArray())

        val mutateMetadata = RetrostashKtorMetadata(
            scopeName = "FeedApi",
            invalidateTemplates = listOf("feed/{id}"),
            bindings = mapOf("id" to "7"),
        )
        runtime.invalidate(mutateMetadata)

        val cached = runtime.resolveFromCache(queryMetadata)
        assertNull(cached)
    }
}
