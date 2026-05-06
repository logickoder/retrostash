import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

group = providers.gradleProperty("POM_GROUP_ID").orElse("dev.logickoder").get()
version = providers.gradleProperty("POM_VERSION").orElse("0.1.0-SNAPSHOT").get()

extensions.configure<KotlinMultiplatformExtension>("kotlin") {
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
    }
}

extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension>("dokka") {
    val moduleMd = layout.projectDirectory.file("module.md")
    if (moduleMd.asFile.exists()) {
        dokkaSourceSets.configureEach {
            includes.from(moduleMd)
        }
    }
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
        // External doc links intentionally minimal: most third-party Kotlin libraries (OkHttp,
        // Retrofit, Ktor) publish Dokka-format docs without a classic `package-list` file, so
        // external lookups 404. Types still resolve from the compile classpath; rendered names
        // just don't deep-link out.
    }
}
