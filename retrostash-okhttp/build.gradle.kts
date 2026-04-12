plugins {
    alias(libs.plugins.android.library)
}

group = providers.gradleProperty("POM_GROUP_ID").orElse("dev.logickoder").get()
version = providers.gradleProperty("POM_VERSION").orElse("0.1.0-SNAPSHOT").get()

android {
    namespace = "dev.logickoder.retrostash.okhttp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
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

dependencies {
    api(project(":retrostash-core"))
    api(project(":retrostash-annotations"))
    implementation(libs.okhttp)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}
