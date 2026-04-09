package dev.logickoder.retrostash

import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Invocation
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class NetworkCacheKeyResolverTest {

    private val resolver = NetworkCacheKeyResolver()

    @Suppress("unused")
    interface TestApi {
        @CacheQuery("users/{id}?q={q}&tenant={tenant}")
        @POST("users/{id}")
        fun search(
            @Path("id") id: String,
            @Query("q") q: String,
            @Body body: SearchRequest
        ): retrofit2.Call<Unit>

        @CacheQuery("users/{id}?missing={missing}")
        @POST("users/{id}")
        fun unresolved(
            @Path("id") id: String,
            @Body body: SearchRequest
        ): retrofit2.Call<Unit>

        @CacheMutate(invalidate = ["users/{id}?q={q}&tenant={tenant}"])
        @POST("users/{id}/mutate")
        fun mutate(
            @Path("id") id: String,
            @Query("q") q: String,
            @Body body: SearchRequest
        ): retrofit2.Call<Unit>

        @CacheQuery("posts/search?userId={userId}")
        @POST("posts")
        fun searchViaPost(@Body body: PostQueryRequest): retrofit2.Call<Unit>

        @CacheMutate(invalidate = ["posts?userId={userId}"])
        @GET("posts/1")
        fun refreshViaGet(@Query("userId") userId: Int): retrofit2.Call<Unit>
    }

    data class SearchRequest(val profile: Profile)
    data class Profile(val tenant: String)
    data class PostQueryRequest(val userId: Int, val title: String)

    @Test
    fun resolves_query_key_from_path_query_and_nested_body_fields() {
        val method = TestApi::class.java.getMethod(
            "search",
            String::class.java,
            String::class.java,
            SearchRequest::class.java
        )
        val invocation =
            Invocation.of(method, listOf("42", "retro", SearchRequest(Profile("acme"))))

        val request = Request.Builder()
            .url("https://example.com/users/42")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .tag(Invocation::class.java, invocation)
            .build()

        val context = resolver.resolveQueryContext(request)

        assertNotNull(context)
        assertTrue(context!!.isPost)
        assertTrue(context.key.startsWith("TestApi|users/42?q=retro&tenant=acme|"))
    }

    @Test
    fun returns_null_when_template_has_unresolvable_placeholders() {
        val method = TestApi::class.java.getMethod(
            "unresolved",
            String::class.java,
            SearchRequest::class.java
        )
        val invocation = Invocation.of(method, listOf("42", SearchRequest(Profile("acme"))))

        val request = Request.Builder()
            .url("https://example.com/users/42")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .tag(Invocation::class.java, invocation)
            .build()

        assertNull(resolver.resolveQueryContext(request))
    }

    @Test
    fun resolves_mutation_invalidation_keys() {
        val method = TestApi::class.java.getMethod(
            "mutate",
            String::class.java,
            String::class.java,
            SearchRequest::class.java
        )
        val invocation =
            Invocation.of(method, listOf("7", "qv", SearchRequest(Profile("tenant-x"))))

        val request = Request.Builder()
            .url("https://example.com/users/7/mutate")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .tag(Invocation::class.java, invocation)
            .build()

        val keys = resolver.resolveMutationKeys(request)

        assertEquals(1, keys.size)
        assertTrue(keys.first().startsWith("TestApi|users/7?q=qv&tenant=tenant-x|"))
    }

    @Test
    fun resolves_post_query_context_for_post_requests() {
        val method = TestApi::class.java.getMethod("searchViaPost", PostQueryRequest::class.java)
        val invocation = Invocation.of(method, listOf(PostQueryRequest(7, "acme")))

        val request = Request.Builder()
            .url("https://example.com/posts")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .tag(Invocation::class.java, invocation)
            .build()

        val context = resolver.resolveQueryContext(request)

        assertNotNull(context)
        assertTrue(context!!.isPost)
        assertTrue(context.key.startsWith("TestApi|posts/search?userId=7|"))
    }

    @Test
    fun resolves_mutation_keys_for_get_requests() {
        val method = TestApi::class.java.getMethod("refreshViaGet", Int::class.java)
        val invocation = Invocation.of(method, listOf(1))

        val request = Request.Builder()
            .url("https://example.com/posts/1?userId=1")
            .get()
            .tag(Invocation::class.java, invocation)
            .build()

        val keys = resolver.resolveMutationKeys(request)

        assertEquals(1, keys.size)
        assertTrue(keys.first().startsWith("TestApi|posts?userId=1|"))
    }
}
