import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = providers.gradleProperty("POM_GROUP_ID").orElse("dev.logickoder").get()
version = providers.gradleProperty("POM_VERSION").orElse("0.1.0-SNAPSHOT").get()

android {
    namespace = "dev.logickoder.retrostash"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
    }
}

dependencies {
    implementation(libs.retrofit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = providers.gradleProperty("POM_GROUP_ID").orElse("dev.logickoder").get()
                artifactId = providers.gradleProperty("POM_ARTIFACT_ID").orElse("retrostash").get()
                version = providers.gradleProperty("POM_VERSION").orElse("0.1.0-SNAPSHOT").get()

                pom {
                    name.set(providers.gradleProperty("POM_NAME").orElse("Retrostash"))
                    description.set(
                        providers.gradleProperty("POM_DESCRIPTION")
                            .orElse("Retrofit + OkHttp query caching and mutation invalidation")
                    )
                    url.set(
                        providers.gradleProperty("POM_URL")
                            .orElse("https://github.com/logickoder/retrostash")
                    )
                }
            }
        }
    }
}