import { useState } from 'react';
import { Globe, Layers, RefreshCw, Zap, type LucideIcon } from 'lucide-react';
import CodeBlock from '../components/CodeBlock';
import Hero from '../components/Hero';
import SEO from '../components/SEO';

const features: Array<{ title: string; body: string; Icon: LucideIcon }> = [
  {
    title: 'Persisted POST query caching',
    body: 'Cache complex non-idempotent payloads — searches, GraphQL, reports — using the same @CacheQuery annotation you use on GET endpoints.',
    Icon: Layers,
  },
  {
    title: 'Mutation-driven invalidation',
    body: '@CacheMutate(invalidate = […]) clears stale entries on a successful 2xx response. Templates resolve placeholders from @Path, @Query, and @Body.',
    Icon: RefreshCw,
  },
  {
    title: 'Converter-agnostic',
    body: 'Reads bytes, not parsed objects. No Gson, Moshi, or kotlinx.serialization lock-in. Works alongside whatever you already use.',
    Icon: Zap,
  },
  {
    title: 'Multiplatform from day one',
    body: 'Core, annotations, and the Ktor plugin run on Android, JVM, iOS, and wasmJs. The OkHttp / Retrofit adapter covers Android + JVM.',
    Icon: Globe,
  },
];

const transports = [
  {
    name: 'Ktor',
    platforms: ['Android', 'JVM', 'iOS', 'Web'],
    blurb: 'KMP HttpClient plugin. Status-gated invalidation, response persistence on 2xx, pluggable logger.',
  },
  {
    name: 'OkHttp',
    platforms: ['Android', 'JVM'],
    blurb: 'Application + network interceptors. Annotation-driven cache writes; mutation- and tag-driven invalidation owns the cache lifecycle.',
  },
  {
    name: 'Retrofit',
    platforms: ['Android', 'JVM'],
    blurb: 'Annotation extractor reads @Path / @Query / @Body and feeds them to the OkHttp interceptor.',
  },
];

const codeSamples: Record<string, { lang: string; code: string }> = {
  retrofit: {
    lang: 'Kotlin · Retrofit',
    code: `interface UserApi {
    @CacheQuery("users/{id}", maxAgeSeconds = 60)
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserDto>

    @CacheMutate(invalidate = ["users/{id}"])
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body body: UserUpdate,
    ): Response<UserDto>
}`,
  },
  okhttp: {
    lang: 'Kotlin · OkHttp',
    code: `val request = Request.Builder()
    .url("https://api.example.com/users/42")
    .get()
    .retrostash(
        OkHttpRetrostashMetadata(
            scopeName = "UserApi",
            queryTemplate = "users/{id}",
            bindings = mapOf("id" to "42"),
            maxAgeMs = 60_000L,
        )
    )
    .build()

client.newCall(request).execute().use { response ->
    // X-Retrostash-Source header tells you cache vs network
}`,
  },
  ktor: {
    lang: 'Kotlin · Ktor (KMP)',
    code: `val client = HttpClient {
    install(RetrostashPlugin) {
        store = InMemoryRetrostashStore()
        timeoutMs = 250
        logger = { println(it) }
    }
}

client.get("https://api.example.com/feed/7") {
    retrostashQuery(
        scopeName = "FeedApi",
        template = "feed/{id}",
        bindings = mapOf("id" to "7"),
        maxAgeMs = 60_000L,
    )
}`,
  },
};

export default function Home() {
  const [activeSample, setActiveSample] = useState<keyof typeof codeSamples>('retrofit');

  return (
    <div>
      <SEO />
      <Hero />

      {/* Feature grid */}
      <section className="mx-auto max-w-5xl px-4 pb-20">
        <div className="grid gap-4 sm:grid-cols-2">
          {features.map((f, i) => (
            <article
              key={f.title}
              className="glow-card scroll-reveal rounded-card border border-outline/30 bg-secondary-container/20 p-6"
              style={{ animationDelay: `${i * 0.05}s` }}
            >
              <div className="mb-3 flex size-10 items-center justify-center rounded-chip bg-primary-container/60 text-on-primary-container">
                <f.Icon className="size-5" aria-hidden />
              </div>
              <h3 className="mb-2 text-base font-semibold text-on-surface">{f.title}</h3>
              <p className="text-sm leading-relaxed text-on-surface-variant">{f.body}</p>
            </article>
          ))}
        </div>
      </section>

      {/* Transports */}
      <section className="mx-auto max-w-5xl px-4 pb-20">
        <div className="mb-8 flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-wider text-primary">
            Pick your transport
          </span>
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Three adapters, one cache
          </h2>
          <p className="max-w-2xl text-on-surface-variant">
            The same RetrostashStore powers all three. Engine adapters hand off cached payloads
            transparently — switch transports without rewriting cache code.
          </p>
        </div>

        <div className="grid gap-3 sm:grid-cols-3">
          {transports.map((t, i) => (
            <article
              key={t.name}
              className="glow-card scroll-reveal rounded-card border border-outline/30 p-6"
              style={{ animationDelay: `${i * 0.06}s` }}
            >
              <p className="mb-1 text-xl font-semibold text-on-surface">{t.name}</p>
              <div className="mb-3 flex flex-wrap gap-1.5">
                {t.platforms.map((p) => (
                  <span
                    key={p}
                    className="rounded-full border border-outline/40 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wider text-on-surface-variant"
                  >
                    {p}
                  </span>
                ))}
              </div>
              <p className="text-sm leading-relaxed text-on-surface-variant">{t.blurb}</p>
            </article>
          ))}
        </div>
      </section>

      {/* Tabbed code sample */}
      <section className="mx-auto max-w-5xl px-4 pb-24">
        <div className="mb-6 flex flex-col gap-2">
          <span className="text-xs font-medium uppercase tracking-wider text-primary">
            30-second taste
          </span>
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Annotate, install, ship
          </h2>
        </div>

        <div className="rounded-card border border-outline/30 bg-secondary-container/20 p-2">
          <div className="mb-2 flex gap-1 border-b border-outline/20 px-3 pt-2 pb-1">
            {(Object.keys(codeSamples) as Array<keyof typeof codeSamples>).map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => setActiveSample(key)}
                className={`relative rounded-t-md px-3 py-2 text-xs font-medium uppercase tracking-wider transition-colors duration-200 ${
                  activeSample === key
                    ? 'text-primary'
                    : 'text-on-surface-variant hover:text-on-surface'
                }`}
              >
                {key}
                {activeSample === key && (
                  <span className="absolute -bottom-px left-0 right-0 h-0.5 rounded-full bg-primary" />
                )}
              </button>
            ))}
          </div>
          <div className="p-3" style={{ animation: 'var(--animate-fade-in)' }} key={activeSample}>
            <CodeBlock lang={codeSamples[activeSample].lang}>
              {codeSamples[activeSample].code}
            </CodeBlock>
          </div>
        </div>
      </section>
    </div>
  );
}
