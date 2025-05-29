// Kotlin DSL (app/build.gradle.kts)
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler) // Compose Compiler Plugin
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    try { // Dosya okuma hatasını yakalamak için try-catch eklenebilir
        FileInputStream(localPropertiesFile).use { fis -> // .use ile otomatik kapatma
            localProperties.load(fis)
        }
    } catch (e: Exception) {
        project.logger.warn("Could not load local.properties file: ${e.message}")
    }
} else {
    project.logger.warn("local.properties file not found. API keys will be empty.")
}

android {
    namespace = "com.example.trafficprediction"
    compileSdk = 35 // Stabil olanı kullanalım

    defaultConfig {
        applicationId = "com.example.trafficprediction"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // API Anahtarlarını resValue ile ekleme (Aynı kalır)
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
        val cfApiKey = localProperties.getProperty("CF_API_KEY", "")
        val openWeatherApiKey = localProperties.getProperty("OPEN_WEATHER_MAP_API_KEY", "") // YENİ
        resValue("string", "google_maps_key", mapsApiKey)
        resValue("string", "cloud_function_key", cfApiKey)
        resValue("string", "openweathermap_api_key", openWeatherApiKey) // YENİ
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles( getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        // buildConfig = false // BuildConfig kullanmıyoruz
    }
    composeOptions {
        // Kotlin 2.0+ olduğu için versiyon belirtmeye gerek yok (plugin tanımında da yok)
        // kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() // Bu satırı sildik
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // --- BOM'ları Uygula ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.play.services.location)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.okhttp.bom))
    // --- ---

    // Core Android & Activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle (Versiyonlar BOM'dan)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose (Versiyonlar BOM'dan)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Navigation (Versiyonlar BOM'dan)
    implementation(libs.androidx.navigation.compose)

    // Firebase (Versiyonlar BOM'dan)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.storage.ktx)
    // implementation(libs.firebase.messaging.ktx) // KALDIRILDI: Firebase Messaging KTX

    // Coil (Resim yükleme için) - versiyonu libs.versions.toml dosyanızda tanımlamanız gerekecek
    implementation(libs.coil.compose) // Takma ad: libs.coil.compose

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences) // Takma ad: libs.androidx.datastore.preferences

    // Retrofit & Gson (Versiyonlar toml'dan)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson) // Takma adı değiştirdim
    implementation(libs.gson) // Gson'ı ayrıca eklemek iyi olabilir

    // Coroutines (Versiyonlar toml'dan)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Maps (Versiyonlar toml'dan)
    implementation(libs.google.maps.compose) // Takma adı değiştirdim
    implementation(libs.google.play.services.maps) // Takma adı değiştirdim
    implementation(libs.google.places) // YENİ: Google Places SDK

    // OkHttp (Versiyonlar BOM'dan)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor) // Takma adı değiştirdim


    // Testing (Hala Devre Dışı)
    // testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.test.ext.junit) // Takma adı değiştirdim
    // androidTestImplementation(libs.androidx.espresso.core)
    // androidTestImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation(libs.androidx.ui.test.manifest)
}
