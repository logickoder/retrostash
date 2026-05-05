# Module retrostash-annotations

The annotation surface that drives Retrostash's caching behavior. Targets Android, JVM, and iOS
(`commonMain`-only — no platform-specific code).

## Entry points

- [`@CacheQuery`][dev.logickoder.retrostash.CacheQuery] — marks a method whose response should be
  cached. Supports a key template, optional TTL, and tag templates for cross-API invalidation.
- [`@CacheMutate`][dev.logickoder.retrostash.CacheMutate] — marks a mutation that invalidates one
  or more cached queries on a successful 2xx response. Invalidation works by key template, by
  tag template, or both.

## See also

- The full README — installation, usage, and the [caching strategy
  guide](https://github.com/logickoder/retrostash#caching-strategy) every OkHttp consumer should
  read before shipping.
- `retrostash-okhttp` — the Retrofit / OkHttp adapter that reads these annotations at runtime.
- `retrostash-ktor` — Ktor client plugin with parallel semantics.
