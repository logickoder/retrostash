# Retrostash

Retrostash is an Android library for annotation-driven query caching with Retrofit + OkHttp.

[![GitHub Release](https://img.shields.io/github/v/release/logickoder/retrostash?label=release)](https://github.com/logickoder/retrostash/releases)
[![JitPack](https://jitpack.io/v/logickoder/retrostash.svg)](https://jitpack.io/#logickoder/retrostash)

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
- `ResponseSourceInterceptor`

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
    implementation("com.github.logickoder:retrostash:0.1.5")
}
```

The README version is updated automatically on release to match the latest tag.

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
val cache = Cache(
    directory = File(appContext.cacheDir, "http-cache"),
    maxSize = 10L * 1024L * 1024L,
)

val okHttpBuilder = OkHttpClient.Builder()
    .cache(cache)

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
        logger = { message -> Log.d("Retrostash", message) },
    )
)

val okHttpClient = okHttpBuilder.build()
```

If you want ordinary GET responses to be stored and reused, keep the OkHttp cache configured.
Retrostash rewrites cache-control headers and handles POST replay plus mutation invalidation, but
OkHttp still owns the actual HTTP cache storage for normal GET caching.

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

When using `@CacheMutate`, include every related query template in `invalidate`, including
POST-based query templates if you use `@CacheQuery` on POST endpoints.

## Clearing Cache

```kotlin
Retrostash.clear(appContext)
```

### Optional logging

Retrostash accepts an optional `logger` callback in `RetrostashConfig`.
Use it when you want visibility into:

- cache-control rewrites
- response source labels
- dirty-key invalidation
- persisted POST cache writes and hits

It is especially useful when you want to confirm whether a response came from Retrostash's
persisted POST cache, the OkHttp cache, or the network.

## Notes

- `NetworkCachePolicyInterceptor` should remain an application interceptor.
- `CacheControlInterceptor` is installed as a network interceptor by `Retrostash.install(...)`.
- For manual wiring, keep this ordering to preserve behavior.

## Contributing and Releases

See [CONTRIBUTING.md](CONTRIBUTING.md) for:

- contribution workflow
- local Maven publishing
- release/versioning flow
- auto-deployment behavior after tags
