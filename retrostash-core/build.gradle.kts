plugins {
    id("retrostash.kmp-library")
}

kotlin {
    jvm()
    android {
        namespace = "dev.logickoder.retrostash.core"
        compileSdk = libs.versions.android.sdk.version.get().toInt()
        minSdk = 21
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            api(project(":retrostash-annotations"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}
