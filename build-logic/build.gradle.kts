plugins {
    `kotlin-dsl`
}

// Plugins applied by precompiled script plugins. Versions match the shared libs.versions.toml.
val kotlinVersion = libs.versions.kotlin.get()
val agpVersion = libs.versions.agp.get()
val vanniktechPublishVersion = libs.versions.vanniktech.publish.get()
val dokkaVersion = libs.versions.dokka.get()

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin:$agpVersion")
    implementation("com.vanniktech:gradle-maven-publish-plugin:$vanniktechPublishVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}
