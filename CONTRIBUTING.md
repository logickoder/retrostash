# Contributing

## Development Setup

1. Clone the repository.
2. Use JDK 17.
3. Run checks:

```bash
./gradlew :retrostash:compileDebugKotlin :retrostash:testDebugUnitTest --console=plain
```

## Local Publishing (Maven Local)

Publish the release artifact locally:

```bash
./gradlew :retrostash:publishReleasePublicationToMavenLocal --console=plain
```

Consume in a local app:

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
// module build.gradle.kts
dependencies {
    implementation("dev.logickoder:retrostash:0.1.0-SNAPSHOT")
}
```

## Release and Versioning

Retrostash uses tag-based releases.

### Create a release

1. Set release version in `gradle.properties`:
    - `POM_VERSION=0.1.0`
2. Commit and push.
3. Tag and push:

```bash
git tag v0.1.0
git push origin v0.1.0
```

4. GitHub Actions runs the release workflow.
5. A GitHub Release is created automatically.
6. Consumers use:

```kotlin
implementation("com.github.logickoder:retrostash:v0.1.0")
```

### Auto-bump after successful tag

After a successful tagged release, CI updates `POM_VERSION` on `main` to the next patch snapshot:

- `v0.1.0` -> `0.1.1-SNAPSHOT`

This keeps development versioning moving automatically.

## JitPack Consumption

Consumers should add JitPack and use the tag coordinate:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// module build.gradle.kts
dependencies {
    implementation("com.github.logickoder:retrostash:<tag>")
}
```

## Pull Request Guidelines

1. Keep changes focused and small.
2. Add or update tests for behavior changes.
3. Keep KDoc current for public APIs.
4. Ensure all module checks pass before opening a PR.
