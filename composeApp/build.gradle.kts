import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    jvm("desktop")

    android {
        namespace = "dev.logickoder.retrostash.example.shared"
        compileSdk = libs.versions.android.sdk.version.get().toInt()
        minSdk = 24
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "RetrostashApp"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "retrostash-app.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.material.icons)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.coroutines.core)
            implementation(libs.ktor.client.core)
            api(project(":retrostash-core"))
            api(project(":retrostash-annotations"))
            api(project(":retrostash-ktor"))
        }

        androidMain {
            kotlin.srcDir("src/jvmAndAndroidMain/kotlin")
            dependencies {
                implementation(libs.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.okhttp)
                implementation(libs.retrofit)
                implementation(project(":retrostash-okhttp"))
            }
        }

        val desktopMain by getting {
            kotlin.srcDir("src/jvmAndAndroidMain/kotlin")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.okhttp)
                implementation(project(":retrostash-okhttp"))
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.logickoder.retrostash.example.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Retrostash Playground"
            packageVersion = "1.0.0"
        }
    }
}
