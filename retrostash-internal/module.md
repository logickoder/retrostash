# Module retrostash-internal

Implementation-detail module shared between the OkHttp and Ktor adapters. **Not part of the
stable API.**

Published to Maven Central so the `retrostash-okhttp` / `retrostash-ktor` POMs resolve
transitively without consumers having to add it manually. Don't depend on this module
directly — every public declaration is annotated
[`@RetrostashInternalApi`][dev.logickoder.retrostash.internal.RetrostashInternalApi] and
requires opt-in to use. Signatures may change in any release without a deprecation cycle.

Exists so the transport adapters can share helpers (placeholder substitution, mutation
invalidation orchestration, binding coercion) without duplicating code or polluting
`retrostash-core`'s public surface.

## Entry points

- [`TemplateResolver`][dev.logickoder.retrostash.internal.TemplateResolver] — `{placeholder}`
  substitution against bindings with JSON-body fallback.
- [`RetrostashEngine.runMutationInvalidations`][dev.logickoder.retrostash.internal.runMutationInvalidations]
  — resolves invalidate-template + invalidate-tag templates and dispatches to the engine.
- [`Map.toStringBindings`][dev.logickoder.retrostash.internal.toStringBindings] — coerces
  `Any?`-valued bindings to the `String`-valued shape `CoreKeyResolver` expects.
- [`RetrostashInternalApi`][dev.logickoder.retrostash.internal.RetrostashInternalApi] —
  opt-in marker.
