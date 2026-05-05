package dev.logickoder.retrostash

/**
 * Marks a Retrofit / Ktor mutation method that should invalidate one or more cached queries on
 * a successful (`2xx`) response.
 *
 * Templates listed in [invalidate] use the same `{placeholder}` syntax as [CacheQuery.key].
 * Placeholders resolve from `@Path`, `@Query`, or `@Body` field names of the mutation call.
 * Non-2xx responses leave the cache untouched.
 *
 * ```kotlin
 * @CacheMutate(invalidate = ["users/{id}?tenant={tenant}"])
 * @PUT("users/{id}")
 * suspend fun updateUser(
 *     @Path("id") id: String,
 *     @Body req: UpdateUserRequest,
 * ): UpdateUserResponse
 * ```
 *
 * ## Tag-based invalidation
 *
 * [invalidateTags] clears every entry whose [CacheQuery.tags] (resolved at cache-write time)
 * contains the resolved tag value. Use it to clear queries across APIs that don't share key
 * templates:
 *
 * ```kotlin
 * @CacheMutate(invalidateTags = ["article:{conceptId}"])
 * @POST("submit_comment")
 * suspend fun submitComment(@Body req: CommentRequest): CommentResponse
 * ```
 *
 * OkHttp users: invalidation here clears Retrostash's store only. If you also use
 * `OkHttpClient.Builder().cache(...)`, that disk cache is **not** evicted — see
 * [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy).
 *
 * @property invalidate Cache key templates to clear after a successful mutation. The templates
 * must match exactly the [CacheQuery.key] templates used by the queries you want invalidated;
 * partial matches do not invalidate.
 * @property invalidates Legacy alias for [invalidate], kept for source compatibility with 0.0.4.
 * Ignored when [invalidate] is non-empty.
 * @property invalidateTags Tag templates to clear after a successful mutation. Each template is
 * resolved against the mutation's bindings / body, then matched against [CacheQuery.tags] stored
 * on cached entries.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(
    val invalidate: Array<String> = [],
    val invalidates: Array<String> = [],
    val invalidateTags: Array<String> = [],
)
