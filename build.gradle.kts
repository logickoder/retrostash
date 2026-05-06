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
