import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(project(":retrostash-core"))
            implementation(project(":retrostash-annotations"))
            implementation(project(":retrostash-ktor"))
        }

        val jvmAndAndroidMain by getting {
            dependencies {
                implementation(libs.okhttp)
                implementation(project(":retrostash-okhttp"))
            }
        }

        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
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

android {
    namespace = "dev.logickoder.retrostash.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.logickoder.retrostash.example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
