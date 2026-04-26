plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
}

group = providers.gradleProperty("POM_GROUP_ID").orElse("dev.logickoder").get()
version = providers.gradleProperty("POM_VERSION").orElse("0.1.0-SNAPSHOT").get()

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "dev.logickoder.retrostash.annotations"
        compileSdk = 36
        minSdk = 21
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
