// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    dokka(project(":retrostash-core"))
    dokka(project(":retrostash-annotations"))
    dokka(project(":retrostash-ktor"))
    dokka(project(":retrostash-okhttp"))
}

dokka {
    moduleName.set("Retrostash")
    pluginsConfiguration.html {
        footerMessage.set(
            "Retrostash · ${providers.gradleProperty("POM_VERSION").getOrElse("snapshot")}"
        )
    }
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension>("dokka") {
            dokkaSourceSets.configureEach {
                jdkVersion.set(17)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
                    )
                )

                externalDocumentationLinks.register("kotlinx-coroutines") {
                    url("https://kotlinlang.org/api/kotlinx.coroutines/")
                }
                // External doc links intentionally minimal: most third-party Kotlin libraries
                // (OkHttp, Retrofit, Ktor) publish Dokka-format docs without a classic
                // `package-list` file, so external lookups 404. Types still resolve from the
                // compile classpath; the rendered names just don't deep-link out.
            }
        }
    }
}
