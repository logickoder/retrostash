# Module retrostash-ktor

Ktor `HttpClient` plugin with parallel semantics to the OkHttp adapter. Targets Android, JVM,
iOS, and wasmJs.

## Entry points

- [`RetrostashPlugin`][dev.logickoder.retrostash.ktor.RetrostashPlugin] — the Ktor client plugin.
  Install via `install(RetrostashPlugin) { store = ...; timeoutMs = ...; logger = ... }`.
- [`RetrostashConfig`][dev.logickoder.retrostash.ktor.RetrostashConfig] — plugin configuration.
- [`RetrostashKtorRuntime`][dev.logickoder.retrostash.ktor.RetrostashKtorRuntime] — adapter that
  bridges per-request metadata to the core engine; exposes `invalidateTag`, `invalidateTags`.
- [`RetrostashKtorMetadata`][dev.logickoder.retrostash.ktor.RetrostashKtorMetadata] and the
  `retrostash`, [`retrostashQuery`][dev.logickoder.retrostash.ktor.retrostashQuery],
  [`retrostashMutate`][dev.logickoder.retrostash.ktor.retrostashMutate] builder extensions for
  attaching cache metadata to a `HttpRequestBuilder`.

## Notes

Ktor's `HttpClient` does not ship a built-in HTTP disk cache the way OkHttp does, so the
dual-cache footgun documented for the OkHttp adapter does not apply here. Retrostash is the
authoritative cache for Ktor consumers.

## See also

- The full README, including [caching
  strategy](https://github.com/logickoder/retrostash#caching-strategy) (OkHttp-specific guidance,
  for context).
- `retrostash-annotations` for `@CacheQuery` / `@CacheMutate`.
- `retrostash-core` for `RetrostashStore` / `RetrostashEngine`.
