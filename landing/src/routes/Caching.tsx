import { ArrowRight, ExternalLink } from 'lucide-react';
import CodeBlock from '../components/CodeBlock';
import SEO from '../components/SEO';

const decisionRows: Array<{ cache: string; flag: string; result: string; tone: 'good' | 'warn' | 'bad' }> = [
  {
    cache: 'No (recommended)',
    flag: 'any',
    result: 'Single source of truth: Retrostash. Invalidation is authoritative.',
    tone: 'good',
  },
  {
    cache: 'Yes',
    flag: 'false',
    result: 'Layered, but origin Cache-Control rules. Stale GET window after invalidation.',
    tone: 'warn',
  },
  {
    cache: 'Yes',
    flag: 'true',
    result: 'Footgun. Up to 24h stale GET window after invalidation. Avoid.',
    tone: 'bad',
  },
];

const toneClass: Record<'good' | 'warn' | 'bad', string> = {
  good: 'text-primary',
  warn: 'text-tertiary',
  bad: 'text-error',
};

const singleCacheSample = `val okHttpBuilder = OkHttpClient.Builder() // no .cache(...)

RetrostashOkHttpAndroid.install(
    builder = okHttpBuilder,
    context = appContext,
)
// Tag and mutation invalidation are authoritative.`;

const layeredSample = `val cache = Cache(File(appContext.cacheDir, "http-cache"), 10L * 1024 * 1024)
val okHttpBuilder = OkHttpClient.Builder().cache(cache)

RetrostashOkHttpAndroid.install(
    builder = okHttpBuilder,
    context = appContext,
    // Stop overriding origin Cache-Control. Accept that Retrostash
    // invalidation does not evict OkHttp HTTP cache entries.
    config = RetrostashOkHttpConfig(enableGetCaching = false),
)`;

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
          <code className="font-mono text-sm">bridge.invalidateTag</code>,{' '}
          <code className="font-mono text-sm">bridge.invalidateQuery</code>) clears the{' '}
          <strong>Retrostash store only</strong>. It does not evict entries from OkHttp's HTTP
          cache.
        </p>
        <p className="mt-3 text-on-surface-variant">
          If <code className="font-mono text-sm">RetrostashOkHttpConfig.enableGetCaching = true</code>{' '}
          (the default) and you also pass <code className="font-mono text-sm">cache(...)</code> to
          your <code className="font-mono text-sm">OkHttpClient.Builder</code>, GETs sit in both
          caches and a post-invalidation GET serves stale from OkHttp — observable as{' '}
          <code className="font-mono text-sm">X-Retrostash-Source: okhttp-cache</code>.
        </p>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">
          What <code className="font-mono">enableGetCaching</code> actually does
        </h2>
        <p className="mt-4 text-on-surface-variant">
          Rewrites outgoing GET response <code className="font-mono text-sm">Cache-Control</code>{' '}
          to <code className="font-mono text-sm">public, max-age=${'$'}{'{'}getMaxAgeSeconds{'}'}</code>{' '}
          (default 24h). It has <em>no effect</em> on Retrostash's own store — Retrostash caches
          based on <code className="font-mono text-sm">@CacheQuery</code>, regardless of this
          flag. It is purely an OkHttp HTTP cache plumbing knob.
        </p>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Two correct configurations</h2>

        <h3 className="mt-6 text-lg font-semibold">Single cache (recommended)</h3>
        <p className="mt-2 text-on-surface-variant">
          Drop OkHttp's <code className="font-mono text-sm">cache(...)</code>. Retrostash is the
          only cache. Tag and mutation invalidation are authoritative.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · Single cache">{singleCacheSample}</CodeBlock>
        </div>

        <h3 className="mt-8 text-lg font-semibold">Layered</h3>
        <p className="mt-2 text-on-surface-variant">
          Keep OkHttp's <code className="font-mono text-sm">Cache(...)</code> only if you
          specifically want OkHttp's <code className="font-mono text-sm">If-None-Match</code> /{' '}
          <code className="font-mono text-sm">304</code> revalidation flow on cold paths. Set{' '}
          <code className="font-mono text-sm">enableGetCaching = false</code> so Retrostash stops
          overriding origin <code className="font-mono text-sm">Cache-Control</code>, and accept
          that Retrostash invalidation does <strong>not</strong> evict OkHttp HTTP cache entries.
        </p>
        <div className="mt-3">
          <CodeBlock lang="Kotlin · Layered">{layeredSample}</CodeBlock>
        </div>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="text-2xl font-semibold tracking-tight">Decision matrix</h2>
        <div className="mt-4 overflow-hidden rounded-card border border-outline/30">
          <table className="w-full border-collapse text-left text-sm">
            <thead className="bg-secondary-container/40 text-on-surface-variant">
              <tr>
                <th className="px-4 py-3 font-medium">Use OkHttp <code className="font-mono">Cache(...)</code> ?</th>
                <th className="px-4 py-3 font-medium"><code className="font-mono">enableGetCaching</code></th>
                <th className="px-4 py-3 font-medium">Result</th>
              </tr>
            </thead>
            <tbody>
              {decisionRows.map((r) => (
                <tr
                  key={`${r.cache}-${r.flag}`}
                  className="border-t border-outline/20 transition-colors duration-150 hover:bg-secondary-container/20"
                >
                  <td className="px-4 py-3 font-mono text-xs text-on-surface">{r.cache}</td>
                  <td className="px-4 py-3 font-mono text-xs text-on-surface-variant">{r.flag}</td>
                  <td className={`px-4 py-3 text-sm ${toneClass[r.tone]}`}>{r.result}</td>
                </tr>
              ))}
            </tbody>
          </table>
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
          — the <code className="font-mono text-sm">RetrostashOkHttpConfig</code> KDoc and the{' '}
          <code className="font-mono text-sm">RetrostashOkHttpInterceptor</code> KDoc both link
          back here.
        </p>
      </div>
    </div>
  );
}
