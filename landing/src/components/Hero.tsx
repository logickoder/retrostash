export default function Hero() {
  return (
    <section className="relative overflow-hidden">
      {/* Animated background orbs */}
      <div aria-hidden className="pointer-events-none absolute inset-0 -z-10">
        <div
          className="absolute -top-32 -left-32 size-[480px] rounded-full bg-primary/20 blur-3xl"
          style={{ animation: 'var(--animate-float)' }}
        />
        <div
          className="absolute top-20 -right-40 size-[520px] rounded-full bg-tertiary/15 blur-3xl"
          style={{ animation: 'var(--animate-float)', animationDelay: '-7s' }}
        />
        <div
          className="absolute -bottom-40 left-1/3 size-[420px] rounded-full bg-secondary-container/40 blur-3xl"
          style={{ animation: 'var(--animate-pulse-slow)' }}
        />
      </div>

      {/* Subtle grid */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 -z-10 opacity-[0.04]"
        style={{
          backgroundImage:
            'linear-gradient(to right, #fff 1px, transparent 1px), linear-gradient(to bottom, #fff 1px, transparent 1px)',
          backgroundSize: '64px 64px',
        }}
      />

      <div className="mx-auto max-w-5xl px-4 pt-20 pb-16 sm:pt-32 sm:pb-24">
        <div
          className="flex flex-col items-start gap-6"
          style={{ animation: 'var(--animate-fade-up)' }}
        >
          <span
            className="inline-flex items-center gap-2 rounded-full border border-outline/40 bg-secondary-container/30 px-3 py-1 text-xs font-medium uppercase tracking-wider text-on-surface-variant backdrop-blur-sm"
            style={{ animation: 'var(--animate-fade-in)' }}
          >
            <span className="relative flex size-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75" />
              <span className="relative inline-flex size-1.5 rounded-full bg-primary" />
            </span>
            Kotlin Multiplatform · Maven Central
          </span>

          <h1
            className="max-w-3xl text-4xl font-semibold leading-[1.05] tracking-tight sm:text-6xl md:text-7xl"
            style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.1s' }}
          >
            Cache Kotlin queries.
            <br />
            <span className="gradient-text">Invalidate on mutation.</span>
          </h1>

          <p
            className="max-w-2xl text-lg leading-relaxed text-on-surface-variant sm:text-xl"
            style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.2s' }}
          >
            Annotation-driven caching for Retrofit, OkHttp, and Ktor. POST queries persist; matching
            mutations clear stale entries automatically. Same API across Android, JVM, iOS, and the
            web.
          </p>

          <div
            className="flex flex-wrap items-center gap-3"
            style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.3s' }}
          >
            <a
              className="shimmer-cta group inline-flex items-center gap-2 rounded-chip px-5 py-3 text-sm font-semibold text-on-primary shadow-lg shadow-primary/20 transition-transform hover:-translate-y-0.5 hover:shadow-primary/40"
              href="/retrostash/playground/"
            >
              Try the live playground
              <span className="transition-transform duration-200 group-hover:translate-x-1">→</span>
            </a>
            <a
              className="inline-flex items-center gap-2 rounded-chip border border-outline/50 bg-surface-variant/30 px-5 py-3 text-sm font-semibold text-on-surface backdrop-blur-sm transition-colors hover:border-primary/60 hover:bg-surface-variant"
              href="/retrostash/api/"
            >
              Read the API docs
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
