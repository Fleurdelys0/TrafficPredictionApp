// Root build.gradle.kts file for our project.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false // If defined in toml.
    alias(libs.plugins.compose.compiler) apply false
    // OR IT CAN STAY LIKE THIS FOR NOW:
    // id("com.google.gms.google-services") version "4.4.2" apply false

    // The Compose Compiler definition SHOULD NOT BE HERE!
}
