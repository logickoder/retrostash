# Module retrostash-okhttp

OkHttp / Retrofit integration: an `Interceptor` that powers `@CacheQuery` and `@CacheMutate` plus
a runtime-reflection extractor for Retrofit annotations. Targets Android and JVM.

## Caching strategy

> Retrostash is your annotation-driven cache. Don't pass `cache(...)` to your
> `OkHttpClient.Builder` unless you specifically want OkHttp's HTTP disk cache for
> origin-`Cache-Control`-driven `If-None-Match` / `304` revalidation — and accept that
> Retrostash invalidation does **not** evict OkHttp HTTP cache entries (treat OkHttp's cache
> like a CDN). After Retrostash invalidation, the next GET can still serve stale from OkHttp's
> HTTP cache (`X-Retrostash-Source: okhttp-cache`).

Read [Caching strategy](https://github.com/logickoder/retrostash#caching-strategy) for the full
guidance.

## Entry points

- [`RetrostashOkHttpBridge`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge] — top-level
  wiring. Holds the engine and store; exposes `install(builder)`, `cache` accessor, and the
  `from(client)` recovery factory.
- [`RetrostashOkHttpCache`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpCache] — direct
  cache surface (peek / update / invalidate / clear), accessed via `bridge.cache`.
- [`RetrostashOkHttpAndroid`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpAndroid] —
  convenience factory that pairs the bridge with an
  [`AndroidRetrostashStore`][dev.logickoder.retrostash.okhttp.AndroidRetrostashStore].
- [`RetrostashOkHttpConfig`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig] — tunables
  (timeouts, store caps, default TTL).
- [`RetrostashOkHttpInterceptor`][dev.logickoder.retrostash.okhttp.RetrostashOkHttpInterceptor] —
  the actual OkHttp `Interceptor`. Documents the `X-Retrostash-Source` header values.
- Request-builder extensions:
  [`retrostashQuery`][dev.logickoder.retrostash.okhttp.retrostashQuery] /
  [`retrostashMutate`][dev.logickoder.retrostash.okhttp.retrostashMutate] for direct OkHttp
  consumers (no Retrofit annotations).

## See also

- `retrostash-annotations` for `@CacheQuery` / `@CacheMutate`.
- `retrostash-core` for `RetrostashStore` / `RetrostashEngine`.
