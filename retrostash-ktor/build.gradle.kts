import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("retrostash.kmp-library")
}

kotlin {
    jvm()
    android {
        namespace = "dev.logickoder.retrostash.ktor"
        compileSdk = 36
        minSdk = 21
    }
    val xcf = XCFramework("Retrostash")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Retrostash"
            isStatic = true
            export(project(":retrostash-core"))
            export(project(":retrostash-annotations"))
            xcf.add(this)
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":retrostash-core"))
            implementation(project(":retrostash-internal"))
            api(libs.ktor.client.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
