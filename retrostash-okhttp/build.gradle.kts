plugins {
    id("retrostash.kmp-library")
}

kotlin {
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
            implementation(project(":retrostash-internal"))
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
