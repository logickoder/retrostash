# Retrostash

**Retrostash** is an annotation-driven caching layer for Retrofit, OkHttp, and Ktor. It solves two
pain points in Kotlin networking: caching non-idempotent queries (like `POST` searches or GraphQL)
and automatically invalidating cached data when mutations occur. Available as a Kotlin Multiplatform
library targeting Android, JVM, and iOS.

[![GitHub Release](https://img.shields.io/github/v/release/logickoder/retrostash?label=release)](https://github.com/logickoder/retrostash/releases)

| Android                                                                                       | iOS                                                                                   |
|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| <img src="docs/screenshots/playground-android.webp" alt="Playground — Android" width="320" /> | <img src="docs/screenshots/playground-ios.webp" alt="Playground — iOS" width="320" /> |
| **Desktop**                                                                                   | **Web (wasmJs)**                                                                      |
| <img src="docs/screenshots/playground-desktop.webp" alt="Playground — Desktop" width="320" /> | <img src="docs/screenshots/playground-web.webp" alt="Playground — Web" width="320" /> |

> Sample app `:composeApp` runs on Android, JVM desktop, iOS, and wasmJs (browser). Switch between
> Ktor, OkHttp, and Retrofit transports via the segmented tab.
>
> 🏠 **[Project site](https://logickoder.dev/retrostash/)** ·
> 🌐 [Live playground](https://logickoder.dev/retrostash/playground/) ·
> 📚 [API docs](https://logickoder.dev/retrostash/api/) ·
> 📦 [APK + Web bundle](https://github.com/logickoder/retrostash/releases)

## Contents
- [Key Features](#key-features)
- [Modules](#modules)
- [Public API](#public-api)
- [Integration](#integration)
- [OkHttp / Retrofit (Android)](#okhttp--retrofit-android)
- [Ktor (KMP)](#ktor-kmp)
- [Template Rules](#template-rules)
- [Clearing Cache](#clearing-cache)
- [Cache API](#cache-api) — peek / update / invalidate / clear
- [Caching strategy](#caching-strategy) — **read this before shipping with OkHttp**
- [Tags: cross-API invalidation](#tags-cross-api-invalidation)
- [Migrating from 0.0.4](#migrating-from-004)
- [Notes](#notes)
- [FAQ](#faq)
- [API documentation](#api-documentation)
- [Contributing and Releases](#contributing-and-releases)

### Key Features
- **Persisted POST query caching:** Safely cache complex payloads like searches and GraphQL.
- **Mutation-driven cache invalidation:** Automatically clear stale data when a user updates a resource.
- **Dynamic key resolution:** Cache templates are resolved directly from `@Path`, `@Query`, and `@Body` parameters.
- **Annotation-driven cache, not a passive HTTP cache.** Retrostash owns the cache lifecycle —
  annotation-controlled writes, mutation- and tag-driven invalidation. Coordinate carefully if
  you also use OkHttp's `Cache(...)` (see [Caching strategy](#caching-strategy)).
- **Multiplatform:** Core engine + annotations + Ktor plugin run on Android, JVM, and iOS. OkHttp
  adapter runs on Android + JVM.

### 100% Converter Agnostic

Retrostash intercepts the raw `RequestBody` (OkHttp) or `HttpRequestBuilder` attributes (Ktor). Key
resolution works with plain Kotlin objects, Maps, Arrays, JSON bytes — no `Gson`/`Moshi`/
`kotlinx.serialization` lock-in.

---

## Modules

| Module                   | Targets                                           | Purpose                                          |
|--------------------------|---------------------------------------------------|--------------------------------------------------|
| `retrostash-core`        | android, jvm, iosX64, iosArm64, iosSimulatorArm64 | Engine, key resolver, in-memory store            |
| `retrostash-annotations` | android, jvm, ios*                                | `@CacheQuery`, `@CacheMutate`                    |
| `retrostash-ktor`        | android, jvm, ios*                                | Ktor `HttpClient` plugin                         |
| `retrostash-okhttp`      | android, jvm                                      | OkHttp interceptor + Retrofit metadata extractor |

---

## Public API

**Primary surface:**
- `@CacheQuery(key = "...", tags = [...])`
- `@CacheMutate(invalidate = [...], invalidateTags = [...])`
- `RetrostashStore`, `InMemoryRetrostashStore`, `RetrostashEngine` (core)
- `RetrostashPlugin`, `retrostashQuery`, `retrostashMutate` (ktor)
- `RetrostashOkHttpBridge`, `RetrostashOkHttpAndroid` (okhttp)

---

## Integration

### Android / JVM (Gradle)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

```kotlin
// module build.gradle.kts
dependencies {
    // Pick ONE transport. Each pulls retrostash-core + retrostash-annotations transitively
    // so you don't add them yourself.
    implementation("dev.logickoder:retrostash-okhttp:0.0.11")
    // or
    implementation("dev.logickoder:retrostash-ktor:0.0.11")
}
```

Need both transports in one project (rare — usually one HTTP stack per app)? Add both `retrostash-okhttp` and `retrostash-ktor`. Don't add `retrostash-core` or `retrostash-annotations` directly — they come along for the ride.

### iOS (Swift Package Manager)

In Xcode: **File → Add Packages…** → enter `https://github.com/logickoder/retrostash` and pick the
version. The `Retrostash` product bundles core + annotations + Ktor plugin as a single XCFramework.

```swift
import Retrostash
```

---

## OkHttp / Retrofit (Android)

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
```

```kotlin
val okHttpBuilder = OkHttpClient.Builder()

val bridge = RetrostashOkHttpAndroid.install(
    builder = okHttpBuilder,
    context = appContext,
    config = RetrostashOkHttpConfig(logger = { Log.d("Retrostash", it) }),
)

val okHttpClient = okHttpBuilder.build()
val sameBridge = RetrostashOkHttpBridge.from(okHttpClient)
```

> **One cache layer.** Don't pass `cache(...)` to the `OkHttpClient.Builder` unless you have a
> specific reason — Retrostash's annotation-driven cache and OkHttp's HTTP disk cache do not
> share an invalidation path. See [Caching strategy](#caching-strategy).

JVM (non-Android) consumers construct `RetrostashOkHttpBridge` directly with their own
`RetrostashStore` impl — no `Context` needed.

## Ktor (KMP)

```kotlin
val store = InMemoryRetrostashStore()
val client = HttpClient {
    install(RetrostashPlugin) {
        this.store = store
        timeoutMs = 250
        logger = { println(it) }
    }
}

client.get("https://api.example.com/feed/7") {
    retrostashQuery(
        scopeName = "FeedApi",
        template = "feed/{id}",
        bindings = mapOf("id" to "7"),
        maxAgeMs = 60_000L,
    )
}

client.post("https://api.example.com/feed/7") {
    retrostashMutate(
        scopeName = "FeedApi",
        invalidateTemplates = listOf("feed/7"),
        bindings = mapOf("id" to "7"),
    )
}
```

---

## Template Rules

Templates use `{placeholder}` syntax. Placeholder sources:
- `@Path("name")`
- `@Query("name")`
- Matching field names found recursively in `@Body`

If any placeholder cannot be resolved, the key is treated as unresolved and the cache action is
skipped safely.

When using `@CacheMutate`, include every related query template in `invalidate`, including POST-based query templates if you use `@CacheQuery` on POST endpoints.

## Clearing Cache

```kotlin
RetrostashOkHttpAndroid.clear(appContext)
// or for any RetrostashStore:
store.clear()
```

## Cache API

Direct cache control lives on a dedicated `cache` accessor on each transport — `bridge.cache`
(OkHttp) and `runtime.cache` (Ktor). Same conceptual surface, ergonomics tuned to the transport.

### Surface

| Verb | OkHttp (blocking, `Class<*>` scope) | Ktor (suspend, `String` scope) |
|---|---|---|
| Read | `bridge.cache.peekQuery(apiClass, template, bindings)` | `runtime.cache.peekQuery(scopeName, template, bindings, bodyBytes?)` |
| Write | `bridge.cache.updateQuery(apiClass, template, bindings, payload, contentType?, maxAgeMs?, tags?)` | `runtime.cache.updateQuery(scopeName, template, bindings, payload, maxAgeMs?, tags?, bodyBytes?)` |
| Invalidate (resolved) | `bridge.cache.invalidateQuery(apiClass, template, bindings)` | `runtime.cache.invalidateQuery(scopeName, template, bindings, bodyBytes?)` |
| Invalidate (raw key) | `bridge.cache.invalidateQueryKey(key)` | `runtime.cache.invalidateQueryKey(key)` |
| Invalidate by tag | `bridge.cache.invalidateTag(tag)` / `invalidateTags(vararg)` | `runtime.cache.invalidateTag(tag)` / `invalidateTags(list)` |
| Clear all | `bridge.cache.clearAll()` | `runtime.cache.clearAll()` |

OkHttp methods block (each call wraps `runBlocking` internally — Android-friendly). Ktor
methods are `suspend` — call from any coroutine.

### `bindings` vs `bodyBytes`

**`bindings`** is a `Map<String, Any?>` of placeholder name → value. These match what `@Path`
and `@Query` parameters provide on annotated endpoints. For most cache calls you supply this
and nothing else.

**`bodyBytes`** is the JSON-encoded request body. Used **only** as a fallback when a
placeholder isn't in `bindings` and must be looked up by JSON field name (Retrostash uses
`Utf8JsonLookup`). Most cache calls leave it `null`.

Example: a `@CacheQuery("posts/{postId}")` on `@POST` with `@Body PostRequest(postId = 1337)`
caches under a key resolved from the body. To peek that entry from outside the request flow,
you must supply the same body bytes:

```kotlin
val req = PostRequest(postId = 1337)
val bodyBytes = Json.encodeToString(req).encodeToByteArray()
bridge.cache.peekQuery(PostApi::class.java, "posts/{postId}", emptyMap(), /* not OkHttp's signature */)
// OkHttp's bridge.cache currently doesn't accept bodyBytes — pass placeholders via bindings.
// Ktor's runtime.cache does accept bodyBytes for parity with the request flow.
```

### Where do the bytes come from?

Retrostash is converter-agnostic — it stores and returns raw bytes. You bring the bytes:

- **Raw `String`:** `payload.encodeToByteArray()`.
- **Retrofit `Response<ResponseBody>`:** `response.body()?.bytes()`.
- **Retrofit `Response<MyDto>`** (typed) — re-serialize:
  ```kotlin
  // kotlinx.serialization
  val bytes = Json.encodeToString(dto).encodeToByteArray()

  // Moshi
  val bytes = moshi.adapter(MyDto::class.java).toJson(dto).encodeToByteArray()

  // Gson
  val bytes = gson.toJson(dto).toByteArray()
  ```
- **Domain object you computed locally** (optimistic UI): same as a typed Response — encode
  with whatever you already use.

### Reading back

`peekQuery` returns the body bytes (envelope unwrapped on OkHttp). Decode with the same
serializer you used to encode:

```kotlin
val raw = bridge.cache.peekQuery(UserApi::class.java, "users/{id}", mapOf("id" to "42"))
    ?: return  // not cached
val user: UserDto = Json.decodeFromString(raw.decodeToString())
```

### Why can't I just pass `MyDto` and let Retrostash serialize?

Coupling to one serializer (kotlinx, Moshi, Gson) would lock every consumer in. The byte
boundary keeps them interchangeable. Recipes are shipped (above); auto-serialization is not.

### Optimistic UI worked example

```kotlin
suspend fun toggleLike(article: Article) {
    // 1. Optimistically update the cached entry the UI reads from
    val newState = article.copy(liked = !article.liked, likeCount = article.likeCount + if (article.liked) -1 else 1)
    val payload = Json.encodeToString(newState).encodeToByteArray()
    bridge.cache.updateQuery(
        apiClass = LikeApi::class.java,
        template = "like_status/{guid}",
        bindings = mapOf("guid" to article.guid),
        payload = payload,
        maxAgeMs = 60_000L,
    )

    // 2. Fire the network mutation; on 2xx, @CacheMutate clears + refetches naturally
    val result = runCatching { likeApi.toggleLike(article.guid) }
    if (result.isFailure) {
        // 3. Roll back: re-write the original
        val rollback = Json.encodeToString(article).encodeToByteArray()
        bridge.cache.updateQuery(
            apiClass = LikeApi::class.java,
            template = "like_status/{guid}",
            bindings = mapOf("guid" to article.guid),
            payload = rollback,
            maxAgeMs = 60_000L,
        )
    }
}
```

### `updateQuery` PATCH semantics

`updateQuery` writes the new `payload` but **preserves** existing entry metadata when an arg is
omitted (`null`). Pass an explicit non-null value to override.

| Param | `null` (default) | Explicit value |
|---|---|---|
| `contentType` (OkHttp) | Keep existing envelope content-type, fall back to `"application/json"` if no entry. | Replace. |
| `maxAgeMs` | Keep existing TTL. Fall back to `0` (no expiry) for new entries. | Replace. `0L` = no expiry. |
| `tags` | Keep existing tags. Fall back to empty for new entries. | `emptyList()` = clear; non-empty = resolve templates and replace. |

The `createdAt` timestamp resets on every patch — a new write restarts the freshness window.

This means optimistic UI updates are tag-safe by default:

```kotlin
// Original: written by @CacheQuery interceptor with tags = ["article:{id}"]
// Now: optimistic patch — payload changes, tags survive
bridge.cache.updateQuery(
    CommentApi::class.java,
    "comment:{container_id}",
    mapOf("container_id" to articleId),
    payload = newJsonBytes,
    // contentType, maxAgeMs, tags all null → preserved
)
// invalidateTag("article:$articleId") still finds + clears this entry.
```

### Footguns

- **Bytes drift from server.** Whatever you write with `updateQuery` is served on every
  subsequent `peekQuery` until the next mutation/invalidation. Wrong bytes = lying cache.
- **Status-code spoofing.** OkHttp's `updateQuery` wraps payloads in a synthetic `200 OK`
  envelope. If consumer code branches on status code, it will always see 200 for cache-hit
  entries you wrote.
- **No ETag / 304 revalidation** on synthetic envelopes — they carry no `ETag` header.
- **Coexistence with OkHttp's `Cache(...)`.** Same caveat as elsewhere: Retrostash invalidation
  doesn't reach OkHttp's HTTP cache. See [Caching strategy](#caching-strategy).

## Caching strategy

Applies to the OkHttp / Retrofit adapter. Ktor users can skip — `HttpClient` ships no built-in
HTTP disk cache, so layering doesn't apply there.

### One cache layer, by design

Retrostash owns its own annotation-driven cache (`@CacheQuery`, `@CacheMutate`, tags). Treat
Retrostash as **the** cache. Don't pass `cache(...)` to your `OkHttpClient.Builder` unless you
have a specific reason and accept the trade-off below.

```kotlin
// Recommended
val okHttpBuilder = OkHttpClient.Builder()  // no .cache(...)
RetrostashOkHttpAndroid.install(builder = okHttpBuilder, context = appContext)
```

### If you also pass `cache(...)` to your builder

OkHttp's HTTP disk cache obeys origin `Cache-Control` headers — separate machinery from
Retrostash's store. Retrostash invalidation (`@CacheMutate`, `bridge.cache.invalidateTag`,
`bridge.cache.invalidateQuery`, `bridge.cache.invalidateQueryKey`) does **not** evict OkHttp
HTTP cache entries. Treat OkHttp's HTTP cache like a CDN you don't control — it serves until
its origin TTL expires. After Retrostash invalidates, the next GET can still hit OkHttp's HTTP
cache; you'll see `X-Retrostash-Source: okhttp-cache` on the response.

If you want OkHttp's HTTP cache for `If-None-Match` / `304 Not Modified` revalidation on the
cold path, that's a fine reason — just know:

- Origin `Cache-Control` headers rule (Retrostash no longer rewrites them).
- POST mutations are tagged `Cache-Control: no-store` so OkHttp doesn't cache mutation responses.
- Retrostash invalidation = Retrostash store only. Plan around it.

```
┌─────────────────────────────┐
│  Retrostash store           │  ← @CacheQuery / @CacheMutate / tags
│  (annotation-driven)        │     Authoritative for Retrostash invalidation.
└──────────────┬──────────────┘
               │ miss
┌──────────────▼──────────────┐
│  OkHttp HTTP cache (Cache)  │  ← Origin-Cache-Control driven.
│  (optional, header-driven)  │     Retrostash never touches it.
└──────────────┬──────────────┘
               │ miss
┌──────────────▼──────────────┐
│  Network                    │
└─────────────────────────────┘
```

## Tags: cross-API invalidation

A single domain object (an article, a user, a workspace) often fans out across unrelated APIs that
each chose their own identifier shape. Tags let those APIs share a logical group without forcing
the consumer to know every key template.

Declare a tag on each `@CacheQuery`. Templates use the same `{placeholder}` syntax as the key and
resolve from the same bindings / body:

```kotlin
@CacheQuery(key = "article:{guid}", tags = ["article:{guid}"])
@GET("article")
suspend fun getArticle(@Query("guid") guid: String): Response<String>

@CacheQuery(key = "like_status:{hostName}:{contentUri}", tags = ["article:{contentUri}"])
@POST("get_like_data")
suspend fun getLikeStatus(@Body request: LikeRequest): Response<List<LikeResponse>>

@CacheQuery(key = "email_alert:{conceptId}", tags = ["article:{conceptId}"])
@GET("checksubscription")
suspend fun getAlertStatus(@Query("conceptId") id: String): Response<EmailAlertResponse>
```

Refresh the article from one place — pass every identifier the article carries:

```kotlin
class ArticleRepository(private val bridge: RetrostashOkHttpBridge) {
    fun invalidateArticle(article: Article) {
        bridge.cache.invalidateTags(
            "article:${article.guid}",
            "article:${article.conceptId}",
            "article:${article.contentUri}",
        )
    }
}
```

Adding a new article-related API later is a one-line annotation change — the refresh call site
stays the same.

A mutation can also clear by tag declaratively:

```kotlin
@CacheMutate(invalidateTags = ["article:{conceptId}"])
@POST("submit_comment")
suspend fun submitComment(@Body req: CommentRequest): Response<CommentResponse>
```

Ktor users have the same surface: `tags` on `retrostashQuery`, `invalidateTags` on
`retrostashMutate`, and `runtime.cache.invalidateTags(listOf(...))` for imperative refresh.

## Migrating from 0.0.4

| Old (0.0.4)                                                | New (0.0.5)                                                              |
|------------------------------------------------------------|--------------------------------------------------------------------------|
| `Retrostash.install(builder, context)`                     | `RetrostashOkHttpAndroid.install(builder, context)`                      |
| `Retrostash.from(client)`                                  | `RetrostashOkHttpBridge.from(client)`                                    |
| `Retrostash.clear(context)`                                | `RetrostashOkHttpAndroid.clear(context)`                                 |
| `RetrostashConfig`                                         | `RetrostashOkHttpConfig` (OkHttp) or `RetrostashConfig` (Ktor)           |
| `PostResponseCacheStore`                                   | `RetrostashStore` + `InMemoryRetrostashStore` / `AndroidRetrostashStore` |
| `NetworkCachePolicyInterceptor`, `CacheControlInterceptor` | merged into `RetrostashOkHttpInterceptor`                                |
| JitPack coords `com.github.logickoder:retrostash`          | Maven Central coords `dev.logickoder:retrostash-*`                       |

## Notes

- For OkHttp, the bridge installs both an application interceptor (handle/marker) and a network
  interceptor (cache-control rewrites). Use `RetrostashOkHttpAndroid.install` to wire them in the
  right order automatically.
- For Ktor, response persistence happens on 2xx only; invalidation also gates on 2xx. Non-2xx
  responses leave the cache untouched.

## FAQ

**Where did `bridge.invalidateQueryKey` / `bridge.invalidateQuery` / `bridge.invalidateTag(s)` go?**

Moved to the dedicated `cache` accessor as a breaking change in 0.0.8:

```kotlin
// before
bridge.invalidateQueryKey(key)
bridge.invalidateQuery(api, template, bindings)
bridge.invalidateTag(tag)
bridge.invalidateTags("a", "b")

// after
bridge.cache.invalidateQueryKey(key)
bridge.cache.invalidateQuery(api, template, bindings)
bridge.cache.invalidateTag(tag)
bridge.cache.invalidateTags("a", "b")
```

Same shape for `runtime.cache.invalidateTag(s)` on Ktor. Full API in [Cache API](#cache-api).

**Why am I still seeing `X-Retrostash-Source: okhttp-cache` after invalidating?**

Your `OkHttpClient.Builder` has `cache(...)` set, and OkHttp's HTTP disk cache still has the
entry — Retrostash invalidation only clears Retrostash's store. See
[Caching strategy](#caching-strategy). Easiest fix: drop `cache(...)` from your builder and let
Retrostash own caching.

**Which TTL knob does what?**

- `@CacheQuery(maxAgeSeconds = ...)` — TTL for that query in **Retrostash's** store.
- `RetrostashOkHttpConfig.defaultMaxAgeMs` — fallback TTL for Retrostash's store when a
  `@CacheQuery` doesn't declare one.
- OkHttp's HTTP cache TTL (when configured separately) follows the origin server's
  `Cache-Control` headers — Retrostash no longer rewrites them.

**Where do I find the API docs?**

- Hosted: [logickoder.dev/retrostash/api/](https://logickoder.dev/retrostash/api/).
- Local: `./gradlew dokkaGenerate` → `build/dokka/html/index.html`.

**Does Retrostash work without Retrofit?**

Yes — use the `retrostashQuery` / `retrostashMutate` extensions on `Request.Builder` (OkHttp) or
`HttpRequestBuilder` (Ktor). Annotations are optional sugar over the same metadata path.

**Can I use a custom store?**

Implement `RetrostashStore` and pass it to `RetrostashOkHttpBridge` / `RetrostashPlugin`. The
in-memory and Android disk stores are reference implementations.

## API documentation

Full Dokka-generated reference at
**[logickoder.dev/retrostash/api/](https://logickoder.dev/retrostash/api/)**. Each module's
landing page summarizes its purpose and links to the most-used types. Generate locally with:

```bash
./gradlew dokkaGenerate
open build/dokka/html/index.html
```

## Contributing and Releases

See [CONTRIBUTING.md](CONTRIBUTING.md) and [development.md](development.md) for:
- Contribution workflow
- Local Maven publishing (`./gradlew publishToMavenLocal`)
- iOS XCFramework build (`./gradlew :retrostash-ktor:assembleRetrostashReleaseXCFramework`)
- Release/versioning flow
