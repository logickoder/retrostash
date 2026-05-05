import { ArrowRight, ExternalLink } from 'lucide-react';
import CodeBlock from '../components/CodeBlock';
import SEO from '../components/SEO';

const singleCacheSample = `val okHttpBuilder = OkHttpClient.Builder() // no .cache(...)

RetrostashOkHttpAndroid.install(
    builder = okHttpBuilder,
    context = appContext,
)
// Retrostash owns the cache lifecycle.
// Tag + mutation invalidation are authoritative.`;

const layeredSample = `val cache = Cache(File(appContext.cacheDir, "http-cache"), 10L * 1024 * 1024)
val okHttpBuilder = OkHttpClient.Builder().cache(cache)

RetrostashOkHttpAndroid.install(builder = okHttpBuilder, context = appContext)

// Origin Cache-Control rules OkHttp's HTTP disk cache.
// Retrostash invalidation does NOT reach it — treat OkHttp's cache like a CDN.`;

const peekSample = `val raw = bridge.cache.peekQuery(
    apiClass = UserApi::class.java,
    template = "users/{id}",
    bindings = mapOf("id" to "42"),
) ?: return  // not cached

val user: UserDto = Json.decodeFromString(raw.decodeToString())`;

const updateSample = `// Optimistic UI: like-toggle that writes the new state to the cache
val newState = article.copy(liked = !article.liked)
val payload = Json.encodeToString(newState).encodeToByteArray()

bridge.cache.updateQuery(
    apiClass = LikeApi::class.java,
    template = "like_status/{guid}",
    bindings = mapOf("guid" to article.guid),
    payload = payload,
    maxAgeMs = 60_000L,
    tags = listOf("article:{guid}"),
)`;

const invalidateSample = `// By query identity (resolves the key for you)
bridge.cache.invalidateQuery(
    UserApi::class.java,
    "users/{id}",
    mapOf("id" to "42"),
)

// By raw cache key (advanced)
bridge.cache.invalidateQueryKey("UserApi|users/42|...")

// By tag — clears every entry whose @CacheQuery.tags resolved to this value
bridge.cache.invalidateTags(
    "article:\${article.guid}",
    "article:\${article.conceptId}",
)`;

const clearSample = `// Drops every Retrostash store entry. Does NOT touch OkHttp's HTTP cache —
// see the Caching strategy section above.
bridge.cache.clearAll()`;

const layersDiagram = `┌─────────────────────────────┐
│  Retrostash store           │  ← @CacheQuery / @CacheMutate / tags
│  (annotation-driven)        │     Authoritative for invalidation.
└──────────────┬──────────────┘
               │ miss
┌──────────────▼──────────────┐
│  OkHttp HTTP cache (Cache)  │  ← Cache-Control driven, opaque to
│  (optional, header-driven)  │     Retrostash invalidation.
└──────────────┬──────────────┘
               │ miss
┌──────────────▼──────────────┐
│  Network                    │
└─────────────────────────────┘`;

export default function Caching() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:py-24">
      <SEO
        title="Caching strategy"
        description="How Retrostash's cache layers with OkHttp's HTTP cache, and how to configure them so invalidation works."
      />
      <div className="scroll-reveal">
        <span className="text-xs font-medium uppercase tracking-wider text-primary">
          Configuration guide
        </span>
        <h1 className="mt-2 text-4xl font-semibold tracking-tight sm:text-5xl">
          Caching strategy
        </h1>
        <p className="mt-4 text-lg text-on-surface-variant">
          Applies to the OkHttp / Retrofit adapter. Ktor users can skip — <code className="font-mono text-sm">HttpClient</code>{' '}
          ships no built-in HTTP disk cache, so the layering trap below does not apply.
        </p>
        <p className="mt-3 text-sm text-on-surface-variant">
          Canonical reference:{' '}
          <a
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
            href="https://github.com/logickoder/retrostash#caching-strategy"
            rel="noreferrer noopener"
            target="_blank"
          >
            README — Caching strategy
            <ExternalLink className="size-3.5" aria-hidden />
          </a>
          . This page is a faster-reading mirror.
        </p>
      </div>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Two cache layers</h2>
        <pre className="mt-4 overflow-x-auto rounded-card border border-outline/30 bg-secondary-container/20 p-5 font-mono text-xs leading-relaxed text-on-surface-variant">
          {layersDiagram}
        </pre>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Why it matters</h2>
        <p className="mt-4 text-on-surface-variant">
          Retrostash invalidation (<code className="font-mono text-sm">@CacheMutate</code>,{' '}
          <code className="font-mono text-sm">bridge.cache.invalidateTag</code>,{' '}
          <code className="font-mono text-sm">bridge.cache.invalidateQuery</code>) clears the{' '}
          <strong>Retrostash store only</strong>. It does not evict entries from OkHttp's HTTP
          cache.
        </p>
        <p className="mt-3 text-on-surface-variant">
          If you pass <code className="font-mono text-sm">cache(...)</code> to your{' '}
          <code className="font-mono text-sm">OkHttpClient.Builder</code>, OkHttp's HTTP cache
          obeys origin <code className="font-mono text-sm">Cache-Control</code> headers
          independently. After Retrostash invalidates, the next GET can still hit OkHttp's HTTP
          cache — observable as{' '}
          <code className="font-mono text-sm">X-Retrostash-Source: okhttp-cache</code>. Treat
          OkHttp's HTTP cache like a CDN you don't control.
        </p>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Recommended configuration</h2>
        <p className="mt-2 text-on-surface-variant">
          Skip <code className="font-mono text-sm">cache(...)</code> on your{' '}
          <code className="font-mono text-sm">OkHttpClient.Builder</code>. Retrostash is the only
          cache. Tag and mutation invalidation are authoritative.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · Single cache (recommended)">{singleCacheSample}</CodeBlock>
        </div>

        <h3 className="mt-8 text-lg font-semibold">If you also configure OkHttp's HTTP cache</h3>
        <p className="mt-2 text-on-surface-variant">
          Origin <code className="font-mono text-sm">Cache-Control</code> headers rule OkHttp's
          HTTP cache — Retrostash no longer rewrites them. Useful if you want{' '}
          <code className="font-mono text-sm">If-None-Match</code> /{' '}
          <code className="font-mono text-sm">304</code> revalidation on cold paths. Trade-off:
          Retrostash invalidation does <strong>not</strong> evict OkHttp HTTP cache entries —
          treat OkHttp's cache like a CDN you don't control. POST mutations are tagged{' '}
          <code className="font-mono text-sm">Cache-Control: no-store</code> so OkHttp won't
          retain mutation responses.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · Layered (origin-driven)">{layeredSample}</CodeBlock>
        </div>
      </section>

      <section className="mt-16 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Direct cache control</h2>
        <p className="mt-4 text-on-surface-variant">
          Beyond declarative <code className="font-mono text-sm">@CacheQuery</code> /{' '}
          <code className="font-mono text-sm">@CacheMutate</code>, both transports expose an
          imperative cache surface for peek, update, invalidate, and clear:{' '}
          <code className="font-mono text-sm">bridge.cache</code> (OkHttp, blocking) and{' '}
          <code className="font-mono text-sm">runtime.cache</code> (Ktor, suspend). Same
          conceptual contract, ergonomics tuned to the transport.
        </p>
        <p className="mt-3 text-sm text-on-surface-variant">
          Canonical reference:{' '}
          <a
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
            href="https://github.com/logickoder/retrostash#cache-api"
            rel="noreferrer noopener"
            target="_blank"
          >
            README — Cache API
            <ExternalLink className="size-3.5" aria-hidden />
          </a>
          . Below is a tour of the four verbs.
        </p>

        <h3 className="mt-8 text-lg font-semibold">Peek a cached entry</h3>
        <p className="mt-2 text-on-surface-variant">
          Returns the body bytes (envelope-unwrapped on OkHttp) or null. Decode with whatever
          serializer you used to write.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · peekQuery">{peekSample}</CodeBlock>
        </div>

        <h3 className="mt-8 text-lg font-semibold">Update / upsert a cached entry</h3>
        <p className="mt-2 text-on-surface-variant">
          Writes the supplied bytes under the resolved cache key. Useful for optimistic UI:
          update local state, push it to the cache, then fire the network mutation.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · updateQuery">{updateSample}</CodeBlock>
        </div>
        <p className="mt-3 text-sm text-on-surface-variant">
          Retrostash is converter-agnostic — bring your own bytes. README has recipes for
          kotlinx.serialization, Moshi, Gson, raw String, and Retrofit{' '}
          <code className="font-mono text-sm">Response&lt;T&gt;</code>.
        </p>

        <h3 className="mt-8 text-lg font-semibold">Invalidate</h3>
        <p className="mt-2 text-on-surface-variant">
          By query identity, by raw key, or by tag.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · invalidate">{invalidateSample}</CodeBlock>
        </div>

        <h3 className="mt-8 text-lg font-semibold">Clear all</h3>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · clearAll">{clearSample}</CodeBlock>
        </div>
      </section>

      <div className="mt-12 rounded-card border border-outline/30 bg-secondary-container/20 p-5 scroll-reveal">
        <p className="text-sm text-on-surface-variant">
          For per-class details, see the{' '}
          <a
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
            href="/retrostash/api/"
          >
            API docs
            <ArrowRight className="size-3.5" aria-hidden />
          </a>{' '}
          — the <code className="font-mono text-sm">RetrostashOkHttpCache</code> /{' '}
          <code className="font-mono text-sm">RetrostashKtorCache</code> KDoc cover every method
          with examples.
        </p>
      </div>
    </div>
  );
}
