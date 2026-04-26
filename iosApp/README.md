# iOS App Shell (developer)

Hosts the Compose Multiplatform `App()` from `:composeApp` on iOS for local development. Not
consumed by
external users — they pull `RetrostashApp` via SPM in [Package.swift](../Package.swift) at the repo
root.

## One-time Xcode project creation

The `.xcodeproj` is **not** in git (Xcode regenerates internal IDs and pbxproj diffs are noisy).
Create it once on a fresh clone:

1. Open Xcode → **File → New → Project…**
2. **iOS** → **App** → Next.
3. Settings:
    - Product Name: `iosApp`
    - Team: your dev team
    - Organization Identifier: `dev.logickoder`
    - Bundle Identifier: `dev.logickoder.retrostash.example`
    - Interface: **SwiftUI**
    - Language: **Swift**
    - Storage: None
4. Save inside this `iosApp/` directory (so the path is `retrostash/iosApp/iosApp.xcodeproj`).
5. Delete Xcode's auto-generated `ContentView.swift` and `iosAppApp.swift`.
   Drag [iOSApp.swift](iOSApp.swift) into the `iosApp` group ("Copy items if needed" off, "Create
   groups" on).

## Build Phase — embed Kotlin framework

In the project navigator: select `iosApp` target → **Build Phases** → `+` → **New Run Script Phase
**. Move it **before** "Compile Sources". Paste:

```bash
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Add Input File: `$(SRCROOT)/../composeApp/build.gradle.kts` (any sentinel; forces Xcode to re-run
the
script when Gradle changes).

## Build Settings

Target → **Build Settings** (toggle "All", "Combined"):

| Setting                  | Value                                                                          |
|--------------------------|--------------------------------------------------------------------------------|
| `Framework Search Paths` | `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` |
| `Other Linker Flags`     | `$(inherited) -framework RetrostashApp`                                        |
| `User Script Sandboxing` | `NO`                                                                           |
| `Enable Module Verifier` | `NO`                                                                           |

## Run

- Pick an iOS Simulator from the scheme dropdown.
- ⌘R to run. First launch invokes Gradle to build the framework — slow. Subsequent runs are fast (
  incremental).

## Troubleshooting

- **"No such module 'RetrostashApp'"** — Gradle script didn't run. Check Build Phase ordering and
  that `User Script Sandboxing = NO`.
- **`embedAndSignAppleFrameworkForXcode` task not found** —
  verify [composeApp/build.gradle.kts](../composeApp/build.gradle.kts) declares iOS targets with
  `binaries.framework { baseName = "RetrostashApp" }`. The Kotlin Multiplatform plugin auto-creates
  the task from those declarations.
- **Linker error "framework not found RetrostashApp"** — `Framework Search Paths` typo. Must match
  the path the Gradle task writes to (`app/build/xcode-frameworks/<Configuration>/<sdk>/`).
- **Stale framework after Kotlin source change** — Xcode caches; **Product → Clean Build Folder** (
  ⇧⌘K), then ⌘R.

## CI iOS shell builds

The `iosApp` shell is **not** wired into the `:release` workflow — only the consumer-facing
`Retrostash.xcframework` (built from `:retrostash-ktor`) is shipped. To smoke-test the shell in CI,
add a
`xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build`
step on a `macos-latest` runner.
