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
 * @property invalidate Cache key templates to clear after a successful mutation. The templates
 * must match exactly the [CacheQuery.key] templates used by the queries you want invalidated;
 * partial matches do not invalidate.
 * @property invalidates Legacy alias for [invalidate], kept for source compatibility with 0.0.4.
 * Ignored when [invalidate] is non-empty.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(
    val invalidate: Array<String> = [],
    val invalidates: Array<String> = [],
)
