# Contributing

## Development Setup

1. Clone the repository.
2. Install JDK 17.
3. macOS required for full builds (iOS targets, XCFramework). Linux/Windows can build android, jvm,
   wasmJs only.
4. Xcode 15+ for iOS work.

## Project Layout

| Module                   | Targets                                             | Purpose                                                                              |
|--------------------------|-----------------------------------------------------|--------------------------------------------------------------------------------------|
| `retrostash-core`        | android, jvm, ios{X64,Arm64,SimulatorArm64}, wasmJs | Engine, key resolver, in-memory store                                                |
| `retrostash-annotations` | android, jvm, ios*, wasmJs                          | `@CacheQuery`, `@CacheMutate`                                                        |
| `retrostash-ktor`        | android, jvm, ios*, wasmJs                          | Ktor `HttpClient` plugin + iOS XCFramework export                                    |
| `retrostash-okhttp`      | android, jvm                                        | OkHttp interceptor + Retrofit metadata                                               |
| `retrostash`             | android                                             | Legacy wrapper module — used as integration smoke-test                               |
| `app`                    | jvm desktop, android library, ios*, wasmJs          | KMP shared module: Compose Multiplatform UI + per-platform actuals                   |
| `androidApp`             | android.application                                 | Android entry point (Activity) — depends on `:app`                                   |
| `iosApp/`                | Xcode project (not Gradle)                          | iOS dev shell hosting `App()` from `:app` (see [iosApp/README.md](iosApp/README.md)) |

## Local Checks

Run the per-module test commands listed in [development.md §4](development.md). Quick
everything-pass:

```bash
./gradlew \
  :retrostash-core:jvmTest :retrostash-core:iosSimulatorArm64Test \
  :retrostash-okhttp:jvmTest \
  :retrostash-ktor:jvmTest :retrostash-ktor:iosSimulatorArm64Test \
  :retrostash-annotations:assemble \
  :app:assembleDebug
```

Run the playground:

```bash
# Android
./gradlew :androidApp:installDebug

# Desktop (JVM)
./gradlew :app:run

# iOS — see iosApp/README.md for one-time Xcode project setup, then ⌘R in Xcode

# Web (wasmJs)
./gradlew :app:wasmJsBrowserDevelopmentRun
```

## Local Publishing (Maven Local)

Publishes per-target artifacts (`-android`, `-jvm`, `-iosx64`, `-iosarm64`, `-iossimulatorarm64`,
`-wasm-js`, root metadata) for every KMP module:

```bash
./gradlew publishToMavenLocal
ls ~/.m2/repository/dev/logickoder/
```

Consume locally:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.logickoder:retrostash-core:0.0.5-SNAPSHOT")
    implementation("dev.logickoder:retrostash-ktor:0.0.5-SNAPSHOT")
    // or
    implementation("dev.logickoder:retrostash-okhttp:0.0.5-SNAPSHOT")
}
```

## Release and Versioning

Releases publish to **Maven Central** (Sonatype Central Portal) for JVM/Android/iOS klibs/wasmJs,
plus a GitHub Release with the iOS `Retrostash.xcframework.zip` for Swift Package Manager
consumption.

### Required CI secrets

Set in repository settings → Secrets:

- `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` — Sonatype Central Portal token (User Token
  format).
- `SIGNING_KEY` — Base64-armored ASCII GPG private key (`gpg --armor --export-secret-keys <ID>`).
- `SIGNING_KEY_ID` — last 8 hex chars of the GPG key fingerprint.
- `SIGNING_KEY_PASSWORD` — passphrase for the GPG key.

### Cut a release

Either tag or use the manual workflow:

**Tag-driven:**
```bash
# 1. Set release version on main
sed -i '' -E "s|^POM_VERSION=.*|POM_VERSION=0.1.0|" gradle.properties
git commit -am "chore: 0.1.0"
git push origin main

# 2. Tag and push
git tag 0.1.0
git push origin 0.1.0
```

**Workflow dispatch:** GitHub → Actions → Release → Run workflow → pick `major`/`minor`/`patch`. CI
computes the next tag, pushes it, and runs the release.

The release job (runs on `macos-latest`):

1. Runs the KMP test suite (jvm + iosSimulator).
2. Verifies `publishToMavenLocal` for every module.
3. Builds `Retrostash.xcframework` and zips it.
4. Patches `Package.swift` with the released version + SHA-256 checksum.
5. Publishes all per-target artifacts to Maven Central via `publishAndReleaseToMavenCentral`.
6. Creates a GitHub Release with `Retrostash.xcframework.zip` attached.

### Auto-bump after release

Post-release, CI bumps `POM_VERSION` on `main` to the next patch snapshot (e.g. `0.1.0` →
`0.1.1-SNAPSHOT`), updates README, and pins `Package.swift` to the released checksum.

## Consumer Coordinates

**Maven Central (Android / JVM / iOS klibs / wasmJs):**
```kotlin
implementation("dev.logickoder:retrostash-core:0.1.0")
implementation("dev.logickoder:retrostash-annotations:0.1.0")
implementation("dev.logickoder:retrostash-ktor:0.1.0")
implementation("dev.logickoder:retrostash-okhttp:0.1.0") // android + jvm only
```

**Swift Package Manager (iOS):**
Add `https://github.com/logickoder/retrostash` in Xcode and pick the desired version. The
`Retrostash` product bundles core + annotations + ktor as a single XCFramework.

JitPack is no longer supported; switch to Maven Central coordinates above.

## Pull Request Guidelines

1. Keep changes focused and small.
2. Add or update tests for behavior changes (see [development.md §1](development.md)).
3. Keep KDoc current for public APIs.
4. Run affected module checks locally before opening the PR.

## Pull Request Checklist (Required)

Before requesting review, confirm:

1. I followed the rules in [development.md](development.md).
2. I added tests for every behavior change in this PR.
3. I added a regression test for each bug fix.
4. I ran affected module checks locally and they passed.
5. I updated docs/KDoc for all public API changes.
6. I did not bundle unrelated refactors with this change.
7. If touching iOS / wasmJs surface, I verified
   `:retrostash-ktor:assembleRetrostashDebugXCFramework` and
   `:app:wasmJsBrowserDevelopmentExecutableDistribution` succeed.

PRs that fail checklist items are not considered merge-ready.
