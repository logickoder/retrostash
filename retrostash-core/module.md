# Module retrostash-core

The transport-agnostic cache engine. KMP-pure code (`commonMain` only) targeting Android, JVM,
iOS, and wasmJs. Used by both the OkHttp/Retrofit and Ktor adapters; can also be used directly
when wiring a custom transport.

## Entry points

- [`RetrostashEngine`][dev.logickoder.retrostash.core.RetrostashEngine] — central orchestrator.
  Owns a [`RetrostashStore`][dev.logickoder.retrostash.core.RetrostashStore] and a
  [`CoreKeyResolver`][dev.logickoder.retrostash.core.CoreKeyResolver]; exposes
  `resolveFromCache`, `persistQueryResult`, `invalidateTemplates`, `invalidateTags`, `clearAll`.
- [`RetrostashStore`][dev.logickoder.retrostash.core.RetrostashStore] — the storage interface.
  Implement it for custom backends; the default
  [`InMemoryRetrostashStore`][dev.logickoder.retrostash.core.InMemoryRetrostashStore] is a
  thread-safe in-memory map suitable for tests and ephemeral caches.
- [`CoreKeyResolver`][dev.logickoder.retrostash.core.CoreKeyResolver] — turns
  [`QueryMetadata`][dev.logickoder.retrostash.core.QueryMetadata] into a stable cache key by
  resolving `{placeholder}` templates from bindings or JSON body fields.
- [`QueryMetadata`][dev.logickoder.retrostash.core.QueryMetadata] — the per-request descriptor:
  scope name, key template, bindings, optional body bytes, and tag templates.

## See also

- The full README — installation, end-to-end examples, [caching
  strategy](https://github.com/logickoder/retrostash#caching-strategy), tags, and the FAQ.
- `retrostash-okhttp` and `retrostash-ktor` for the transport adapters.
