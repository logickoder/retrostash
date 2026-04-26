package dev.logickoder.retrostash

/**
 * Marks a Retrofit / Ktor query method whose response should be cached by Retrostash.
 *
 * The [key] template uses `{placeholder}` syntax. Placeholders resolve from `@Path`, `@Query`, or
 * matching field names in `@Body`. If any placeholder cannot be resolved at request time, the
 * cache action is skipped safely — the call hits the network as if uncached.
 *
 * ```kotlin
 * @CacheQuery("users/{id}?tenant={tenant}", maxAgeSeconds = 60)
 * @POST("users/{id}")
 * suspend fun getUser(
 *     @Path("id") id: String,
 *     @Body req: UserRequest,
 * ): UserResponse
 * ```
 *
 * @property key Template that, after placeholder substitution, identifies the cached entry.
 * Must be unique per logical query. Must match the templates used in [CacheMutate.invalidate]
 * for invalidation to fire.
 * @property maxAgeSeconds TTL for the cached payload. `0` (default) means no explicit expiry —
 * the entry lives until invalidated by a matching [CacheMutate], cleared explicitly, or evicted
 * by capacity rules in the underlying store.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheQuery(
    val key: String,
    val maxAgeSeconds: Long = 0L,
)
