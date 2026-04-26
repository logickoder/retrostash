import { Apple, Boxes, ExternalLink, Tag, TerminalSquare } from 'lucide-react';
import CodeBlock from '../components/CodeBlock';
import SEO from '../components/SEO';

const RELEASES_URL = 'https://github.com/logickoder/retrostash/releases/latest';

export default function Install() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:py-24">
      <SEO
        title="Install"
        description="Maven Central + Swift Package Manager coordinates for Retrostash."
      />
      <div className="scroll-reveal">
        <span className="text-xs font-medium uppercase tracking-wider text-primary">
          Setup
        </span>
        <h1 className="mt-2 text-4xl font-semibold tracking-tight sm:text-5xl">Install</h1>
        <p className="mt-4 text-lg text-on-surface-variant">
          Coordinates published to Maven Central. iOS available via Swift Package Manager. Pick
          the modules you need — there's no monolith.
        </p>
      </div>

      <a
        href={RELEASES_URL}
        target="_blank"
        rel="noreferrer noopener"
        className="mt-6 inline-flex items-center gap-2 rounded-card border border-primary/30 bg-primary-container/30 px-4 py-3 text-sm transition-colors hover:border-primary/60 hover:bg-primary-container/50"
      >
        <Tag className="size-4 text-primary" aria-hidden />
        <span className="text-on-surface">
          Replace <code className="font-mono text-on-primary-container">{'<LATEST>'}</code> with the
          newest tag — see GitHub releases
        </span>
        <ExternalLink className="size-3.5 text-on-surface-variant" aria-hidden />
      </a>

      <section className="mt-12 scroll-reveal">
        <h2 className="mb-4 flex items-center gap-2 text-xl font-semibold">
          <Boxes className="size-5 text-primary" aria-hidden />
          Android · JVM · wasmJs
        </h2>
        <div className="space-y-3">
          <CodeBlock lang="settings.gradle.kts">{`dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}`}</CodeBlock>

          <CodeBlock lang="build.gradle.kts">{`dependencies {
    implementation("dev.logickoder:retrostash-core:<LATEST>")
    implementation("dev.logickoder:retrostash-annotations:<LATEST>")

    // Pick a transport (or both):
    implementation("dev.logickoder:retrostash-okhttp:<LATEST>") // android + jvm
    implementation("dev.logickoder:retrostash-ktor:<LATEST>")   // android + jvm + ios + wasmJs
}`}</CodeBlock>
        </div>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="mb-4 flex items-center gap-2 text-xl font-semibold">
          <Apple className="size-5 text-tertiary" aria-hidden />
          iOS · Swift Package Manager
        </h2>
        <p className="mb-3 text-sm text-on-surface-variant">
          In Xcode: <strong className="text-on-surface">File → Add Packages…</strong> and enter the
          repo URL. Pick the latest version from{' '}
          <a
            href={RELEASES_URL}
            target="_blank"
            rel="noreferrer noopener"
            className="text-primary hover:underline"
          >
            GitHub releases
          </a>
          . The{' '}
          <code className="rounded bg-secondary-container/40 px-1.5 py-0.5 font-mono text-xs">
            Retrostash
          </code>{' '}
          product bundles core + annotations + Ktor as a single XCFramework.
        </p>
        <div className="space-y-3">
          <CodeBlock lang="Package URL">{`https://github.com/logickoder/retrostash`}</CodeBlock>

          <CodeBlock lang="Swift">{`import Retrostash

let store = InMemoryRetrostashStore()
let runtime = RetrostashKtorRuntime.companion.create(
    store: store,
    timeoutMs: 250
)`}</CodeBlock>
        </div>
      </section>

      <section className="mt-12 scroll-reveal">
        <h2 className="mb-4 flex items-center gap-2 text-xl font-semibold">
          <TerminalSquare className="size-5 text-secondary" aria-hidden />
          Verify
        </h2>
        <CodeBlock>{`./gradlew dependencies --configuration releaseRuntimeClasspath \\
  | grep retrostash`}</CodeBlock>
      </section>
    </div>
  );
}
