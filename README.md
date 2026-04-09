# Retrostash

**Retrostash** is an annotation-driven caching layer for Retrofit and OkHttp. It solves two massive pain points in Android networking: caching non-idempotent queries (like `POST` searches or GraphQL) and automatically invalidating cached data when mutations (like `POST` or `PUT` updates) occur.

[![GitHub Release](https://img.shields.io/github/v/release/logickoder/retrostash?label=release)](https://github.com/logickoder/retrostash/releases)
[![JitPack](https://jitpack.io/v/logickoder/retrostash.svg)](https://jitpack.io/#logickoder/retrostash)

### Key Features
- **Persisted POST query caching:** Safely cache complex payloads like searches and GraphQL.
- **Mutation-driven cache invalidation:** Automatically clear stale data when a user updates a resource.
- **Dynamic key resolution:** Cache templates are resolved directly from `@Path`, `@Query`, and `@Body` parameters.
- **HTTP cache friendliness:** Includes GET cache header policies that work seamlessly with OkHttp.

### 100% Converter Agnostic
Retrostash intentionally avoids `Gson` or `Moshi` lock-in by intercepting the raw OkHttp `RequestBody`. Key resolution works seamlessly with plain Kotlin/Java objects, Maps, Arrays, and Android `JSONObject`, regardless of your chosen serialization library.

---

## Public API

**Primary API surface:**
- `@CacheQuery(key = "...")`
- `@CacheMutate(invalidate = ["..."])`
- `Retrostash.install(...)`
- `Retrostash.clear(...)`
- `Retrostash.invalidateQuery(...)`
- `Retrostash.invalidateQueryKey(...)`
- `RetrostashConfig`

*Advanced classes remain available for manual wiring:*
- `NetworkCacheKeyResolver`
- `NetworkCacheInvalidator`
- `PostResponseCacheStore`
- `NetworkCachePolicyInterceptor`
- `CacheInterceptor`

## How It Works

1. Query methods are annotated with `@CacheQuery` and a template key.
2. Mutation methods are annotated with `@CacheMutate` and invalidation templates.
3. On successful mutation responses, resolved invalidation keys are marked dirty.
4. Dirty query keys force a network refresh on the next call.
5. Successful POST query responses can be persisted and replayed when the key is clean.

---

## Integration (Recommended)

### 1) Add dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("[https://jitpack.io](https://jitpack.io)")
    }
}
```

```kotlin
// module build.gradle.kts
dependencies {
    implementation("com.github.logickoder:retrostash:1.0.0")
}
```

*The README version is updated automatically on release to match the latest tag.*

### 2) Annotate Retrofit service

```kotlin
interface UserApi {
    
    // 1. A complex POST query that we want to cache locally
    @CacheQuery("users/{id}?tenant={tenant}")
    @POST("users/{id}")
    suspend fun getUser(
        @Path("id") id: String,
        @Body req: UserRequest, // Retrostash automatically extracts {tenant} from this body
    ): UserResponse

    // 2. A mutation that immediately invalidates the cache from #1
    @CacheMutate(invalidate = ["users/{id}?tenant={tenant}"])
    @POST("users/{id}/update")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body req: UpdateUserRequest, // Retrostash builds the invalidation key from these parameters
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

// The simplest setup uses sane defaults:
Retrostash.install(
    builder = okHttpBuilder,
    context = appContext
)

// Or customize TTLs, sizes, and logging:
/*
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
*/

val okHttpClient = okHttpBuilder.build()
```

If you want ordinary GET responses to be stored and reused, keep the OkHttp cache configured. Retrostash rewrites cache-control headers and handles POST replay plus mutation invalidation, but OkHttp still owns the actual HTTP cache storage for normal GET caching.

### 4) Build Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("[https://api.example.com/](https://api.example.com/)")
    .client(okHttpClient)
    .build()
```

---

## Template Rules

Templates use `{placeholder}` syntax.

Placeholder sources:
- `@Path("name")`
- `@Query("name")`
- Matching field names found recursively in `@Body`

If any placeholder cannot be resolved, the key is treated as unresolved and that cache action is skipped safely.

When using `@CacheMutate`, include every related query template in `invalidate`, including POST-based query templates if you use `@CacheQuery` on POST endpoints.

## Clearing Cache

```kotlin
Retrostash.clear(appContext)
```

## External Invalidation

If you need to invalidate outside interceptor flow, Retrostash can mark query keys dirty directly.

**By template plus bindings:**
```kotlin
Retrostash.invalidateQuery(
    context = appContext,
    apiClass = UserApi::class.java,
    template = "users/{id}?tenant={tenant}",
    bindings = mapOf(
        "id" to "42",
        "tenant" to "acme",
    ),
)
```

**By resolved internal key:**
```kotlin
Retrostash.invalidateQueryKey(
    context = appContext,
    key = "UserApi|users/42?tenant=acme|...",
)
```

This applies to both GET and POST queries annotated with `@CacheQuery`.

### Optional Logging

Retrostash accepts an optional `logger` callback in `RetrostashConfig`. Use it when you want visibility into:
- Cache-control rewrites
- Response source labels
- Dirty-key invalidations
- Persisted POST cache writes and hits

It is especially useful when you want to confirm whether a response came from Retrostash's persisted POST cache, the standard OkHttp cache, or the network.

---

## Notes

- `NetworkCachePolicyInterceptor` should remain an application interceptor.
- `CacheControlInterceptor` is installed as both an application and network interceptor by `Retrostash.install(...)`.
- For manual wiring, keep this ordering to preserve behavior.

## Contributing and Releases

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Contribution workflow
- Local Maven publishing
- Release/versioning flow
- Auto-deployment behavior after tags
```