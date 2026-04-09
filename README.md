# Retrostash

Retrostash is an Android library for annotation-driven query caching with Retrofit + OkHttp.

It provides:

- GET cache header policy for HTTP cache friendliness
- persisted POST query caching
- mutation-driven cache invalidation
- cache key templates resolved from `@Path`, `@Query`, and `@Body`

The module intentionally avoids Gson lock-in. Key resolution works with plain Kotlin/Java objects,
maps/arrays/iterables, and Android `JSONObject` / `JSONArray`.

## Public API

Primary API surface:

- `@CacheQuery(key = "...")`
- `@CacheMutate(invalidate = ["..."])`
- `Retrostash.install(...)`
- `Retrostash.create(...)`
- `Retrostash.clear(...)`
- `RetrostashConfig`

Advanced classes remain available for manual wiring:

- `NetworkCacheKeyResolver`
- `NetworkCacheInvalidator`
- `PostResponseCacheStore`
- `NetworkCachePolicyInterceptor`

## How It Works

1. Query methods are annotated with `@CacheQuery` and a template key.
2. Mutation methods are annotated with `@CacheMutate` and invalidation templates.
3. On successful mutation responses, resolved invalidation keys are marked dirty.
4. Dirty query keys force a network refresh.
5. Successful POST query responses can be persisted and replayed when key is clean.

## Integration (Recommended)

### 1) Add dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// module build.gradle.kts
dependencies {
    implementation("com.github.logickoder:retrostash:<tag>")
}
```

Example:

- `implementation("com.github.logickoder:retrostash:v0.1.0")`

### 2) Annotate Retrofit service

```kotlin
interface UserApi {
    @CacheQuery("users/{id}?tenant={tenant}")
    @POST("users/{id}")
    suspend fun getUser(
        @Path("id") id: String,
        @Body req: UserRequest,
    ): UserResponse

    @CacheMutate(invalidate = ["users/{id}?tenant={tenant}"])
    @POST("users/{id}/update")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body req: UpdateUserRequest,
    ): UpdateUserResponse
}

data class UserRequest(val tenant: String)
```

### 3) Install Retrostash in OkHttp

```kotlin
val okHttpBuilder = OkHttpClient.Builder()

Retrostash.install(
    builder = okHttpBuilder,
    context = appContext,
    config = RetrostashConfig(
        getMaxAgeSeconds = 60 * 60,
        enableGetCaching = true,
        invalidationTtlMs = 24 * 60 * 60 * 1000L,
        postCacheMaxEntries = 64,
        postCacheMaxBytes = 4 * 1024 * 1024L,
        postCacheTtlMs = 15 * 60 * 1000L,
    )
)

val okHttpClient = okHttpBuilder.build()
```

### 4) Build Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(okHttpClient)
    .build()
```

## Template Rules

Templates use `{placeholder}` syntax.

Placeholder sources:

- `@Path("name")`
- `@Query("name")`
- matching field names found recursively in `@Body`

If any placeholder cannot be resolved, the key is treated as unresolved and that cache action is
skipped.

## Clearing Cache

```kotlin
Retrostash.clear(appContext)
```

## Notes

- `NetworkCachePolicyInterceptor` should remain an application interceptor.
- `CacheInterceptor` is installed as a network interceptor by `Retrostash.install(...)`.
- For manual wiring, keep this ordering to preserve behavior.

## Contributing and Releases

See [CONTRIBUTING.md](CONTRIBUTING.md) for:

- contribution workflow
- local Maven publishing
- release/versioning flow
- auto-deployment behavior after tags
