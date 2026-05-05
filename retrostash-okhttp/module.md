# Module retrostash-okhttp

OkHttp / Retrofit integration: an `Interceptor` that powers `@CacheQuery` and `@CacheMutate` plus
a runtime-reflection extractor for Retrofit annotations. Targets Android and JVM.

## Caching strategy

> Retrostash's store and OkHttp's HTTP cache (`OkHttpClient.Builder().cache(...)`) are two
> different caches. Retrostash invalidation does **not** evict OkHttp HTTP cache entries. If
> [`enableGetCaching`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig] is `true` (the
> default) and you also pass `cache(...)` to your `OkHttpClient.Builder`, GETs sit in both caches
> and a post-invalidation request can serve stale from OkHttp (`X-Retrostash-Source:
> okhttp-cache`).

Read [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy) before
shipping. The two correct configurations are documented there.

## Entry points

- [`RetrostashOkHttpBridge`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge] — top-level
  wiring. Holds the engine and store; exposes `install(builder)`, `invalidateQuery`,
  `invalidateQueryKey`, `invalidateTag`, `invalidateTags(vararg)`, and the `from(client)`
  recovery factory.
- [`RetrostashOkHttpAndroid`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpAndroid] —
  convenience factory that pairs the bridge with an
  [`AndroidRetrostashStore`][dev.logickoder.retrostash.okhttp.AndroidRetrostashStore].
- [`RetrostashOkHttpConfig`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig] — tunables.
  See its KDoc for the cache-layering caveat on `enableGetCaching`.
- [`RetrostashOkHttpInterceptor`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpInterceptor] —
  the actual OkHttp `Interceptor`. Documents the `X-Retrostash-Source` header values.
- Request-builder extensions:
  [`retrostashQuery`][dev.logickoder.retrostash.okhttp.retrostashQuery] /
  [`retrostashMutate`][dev.logickoder.retrostash.okhttp.retrostashMutate] for direct OkHttp
  consumers (no Retrofit annotations).

## See also

- `retrostash-annotations` for `@CacheQuery` / `@CacheMutate`.
- `retrostash-core` for `RetrostashStore` / `RetrostashEngine`.
