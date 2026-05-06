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
        // Stamp output `.class` files with the older 2.0.0 metadata format so consumers using
        // kapt + Room (which bundles `kotlin-metadata-jvm` ≤ 2.2) can read Retrostash's classes.
        // Without this, Kotlin 2.3 compiler emits metadata 2.3.0 → kapt fails with
        // "Provided Metadata instance has version 2.3.0, while maximum supported version is 2.2.0".
        freeCompilerArgs.addAll("-Xmetadata-version=2.0.0", "-Xsuppress-version-warnings")
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
