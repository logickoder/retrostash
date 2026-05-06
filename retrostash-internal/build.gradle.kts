plugins {
    id("retrostash.kmp-library")
}

// Published as a regular Maven artifact so the okhttp/ktor POMs resolve cleanly. All public
// declarations are marked `@RetrostashInternalApi` (opt-in error) so consumers cannot use them
// without explicit ceremony — the module name + opt-in is the API contract.

kotlin {
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
