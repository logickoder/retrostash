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
 * ## Tags for cross-API invalidation
 *
 * [tags] declares logical group identifiers stored alongside the cache entry. Each tag template
 * uses the same `{placeholder}` syntax as [key] and is resolved from the same sources (bindings
 * → JSON body). Unresolvable tags are dropped silently — the entry is still cached.
 *
 * Tags let one mutation invalidate queries across unrelated APIs without sharing key shapes:
 *
 * ```kotlin
 * // Three different APIs, three different identifiers, one shared tag namespace:
 * @CacheQuery(key = "user:{id}", tags = ["user:{id}"])
 * suspend fun getUser(@Path("id") id: String): UserResponse
 *
 * @CacheQuery(key = "like_status:{hostName}:{contentUri}", tags = ["user:{contentUri}"])
 * suspend fun getLikeStatus(@Body req: LikeRequest): LikeResponse
 *
 * @CacheQuery(key = "email_alert:{conceptId}", tags = ["user:{conceptId}"])
 * suspend fun getAlertStatus(@Query("conceptId") id: String): EmailAlertResponse
 * ```
 *
 * The consumer invalidates all three with a single repository call — see
 * `RetrostashOkHttpBridge.invalidateTags` / `RetrostashKtorRuntime.invalidateTags`.
 *
 * @property key Template that, after placeholder substitution, identifies the cached entry.
 * Must be unique per logical query. Must match the templates used in [CacheMutate.invalidate]
 * for invalidation to fire.
 * @property maxAgeSeconds TTL for the cached payload. `0` (default) means no explicit expiry —
 * the entry lives until invalidated by a matching [CacheMutate], cleared explicitly, or evicted
 * by capacity rules in the underlying store.
 * @property tags Optional logical group identifiers stored with the entry. Each template is
 * resolved from the same bindings as [key]. An imperative `invalidateTag(...)` call on the
 * runtime bridge clears every entry whose resolved tag set contains the given value.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheQuery(
    val key: String,
    val maxAgeSeconds: Long = 0L,
    val tags: Array<String> = [],
)
