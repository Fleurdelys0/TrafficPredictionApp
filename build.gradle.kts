// Kök build.gradle.kts

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false // Eğer toml'da tanımlıysa
    alias(libs.plugins.compose.compiler) apply false
    // VEYA ŞİMDİLİK BÖYLE KALABİLİR:
    // id("com.google.gms.google-services") version "4.4.2" apply false

    // Compose Compiler tanımı BURADA OLMAMALI!
}