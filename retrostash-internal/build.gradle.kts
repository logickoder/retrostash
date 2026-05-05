plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.dokka)
}

// Internal-only module — intentionally NOT applying the vanniktech publish plugin.
// retrostash-okhttp and retrostash-ktor consume it via `implementation` so it never reaches
// downstream consumers' classpaths.

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "dev.logickoder.retrostash.internal"
        compileSdk = 36
        minSdk = 21
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":retrostash-core"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
