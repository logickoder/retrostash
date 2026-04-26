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
        namespace = "dev.logickoder.retrostash.okhttp"
        compileSdk = 36
        minSdk = 21
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":retrostash-core"))
            api(project(":retrostash-annotations"))
            implementation(libs.coroutines.core)
            api(libs.okhttp)
            implementation(libs.retrofit)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.coroutines.test)
        }
    }
}
