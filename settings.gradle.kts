// settings.gradle.kts

// This block defines how Gradle will manage plugins and their versions.
pluginManagement {
    // 1. Repositories where plugins will be searched for.
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // 2. IDs and versions of the main plugins to be used.
    //    Versions defined here allow us to add plugins by ID only in module-level build.gradle.kts files,
    //    without specifying the version again.
    plugins {
        // Android application plugin and its version.
        id("com.android.application") version "8.2.2" apply false // Make sure to use your AGP version.
        // Kotlin Android plugin and its version.
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false // Make sure to use your Kotlin version.
        // Compose Compiler plugin and its version (should be compatible with the Kotlin version).
        id("org.jetbrains.kotlin.plugin.compose") apply false // Appropriate version for Kotlin 1.9.22 (or yours).
        // Google Services plugin (if defined in the root project).
        // Usually defined here; check the version.
        id("com.google.gms.google-services") version "4.4.2" apply false // Make sure to use your version.
    }
}

// This block defines where the project's dependencies (libraries) will be found.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Other repositories (e.g., JitPack) can be added here if needed.
    }
}

rootProject.name = "TrafficPrediction" // Check the project name.
include(":app")                  // Include the application module.
