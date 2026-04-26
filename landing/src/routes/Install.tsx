import { Apple, Boxes, TerminalSquare } from 'lucide-react';
import CodeBlock from '../components/CodeBlock';
import SEO from '../components/SEO';

export default function Install() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:py-24">
      <SEO
        title="Install"
        description="Maven Central + Swift Package Manager coordinates for Retrostash 0.0.5+."
      />
      <div className="scroll-reveal">
        <span className="text-xs font-medium uppercase tracking-wider text-primary">
          Setup
        </span>
        <h1 className="mt-2 text-4xl font-semibold tracking-tight sm:text-5xl">Install</h1>
        <p className="mt-4 text-lg text-on-surface-variant">
          Coordinates published to Maven Central as of 0.0.5. iOS available via Swift Package
          Manager. Pick the modules you need — there's no monolith.
        </p>
      </div>

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
    implementation("dev.logickoder:retrostash-core:0.0.5")
    implementation("dev.logickoder:retrostash-annotations:0.0.5")

    // Pick a transport (or both):
    implementation("dev.logickoder:retrostash-okhttp:0.0.5") // android + jvm
    implementation("dev.logickoder:retrostash-ktor:0.0.5")   // android + jvm + ios + wasmJs
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
          repo URL. The <code className="rounded bg-secondary-container/40 px-1.5 py-0.5 font-mono text-xs">Retrostash</code>{' '}
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
