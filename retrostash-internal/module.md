# Module retrostash-internal

Implementation-detail module shared between the OkHttp and Ktor adapters. **Not published to
Maven Central** and **not part of the stable API.**

Consumers should never depend on this module. It exists so the OkHttp and Ktor transport
adapters can share helpers (placeholder substitution, future cross-adapter utilities) without
duplicating code or polluting the public surface of `retrostash-core`.

Everything in this module is annotated [`@RetrostashInternalApi`][dev.logickoder.retrostash.internal.RetrostashInternalApi]
and requires opt-in to use, signaling that signatures may change in any release.

## Entry points

- [`TemplateResolver`][dev.logickoder.retrostash.internal.TemplateResolver] — `{placeholder}`
  substitution against bindings with JSON-body fallback.
- [`RetrostashInternalApi`][dev.logickoder.retrostash.internal.RetrostashInternalApi] —
  opt-in marker.
