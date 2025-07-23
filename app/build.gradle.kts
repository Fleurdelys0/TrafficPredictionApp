import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler) // We're enabling the Compose Compiler Plugin here.
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    try { // We're adding a try-catch here to handle potential file reading errors.
        FileInputStream(localPropertiesFile).use { fis -> // We use .use for automatic resource management (closing the stream).
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
    compileSdk = 35 // We're using a stable version for compileSdk.

    defaultConfig {
        applicationId = "com.example.trafficprediction"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Here, we're adding API keys as resource values.
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
        val cfApiKey = localProperties.getProperty("CF_API_KEY", "")
        val openWeatherApiKey = localProperties.getProperty("OPEN_WEATHER_MAP_API_KEY", "")
        resValue("string", "google_maps_key", mapsApiKey)
        resValue("string", "cloud_function_key", cfApiKey)
        resValue("string", "openweathermap_api_key", openWeatherApiKey)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        // buildConfig = false // We're not using BuildConfig.
    }
    composeOptions {
        // Since we're using Kotlin 2.0+, we don't need to specify the compiler extension version.
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Applying Bill of Materials (BOMs) for dependency management.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.play.services.location)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.firebase.crashlytics.buildtools)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.okhttp.bom))

    // Core Android and Activity Compose dependencies.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle dependencies (versions managed by BOM).
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose dependencies (versions managed by BOM).
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Navigation Compose dependency (version managed by BOM).
    implementation(libs.androidx.navigation.compose)

    // Firebase dependencies (versions managed by BOM).
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging) // Firebase Cloud Messaging (alias from libs.versions.toml).

    // AndroidX WorkManager for background tasks.
    implementation(libs.androidx.work.runtime.ktx)

    // Coil for image loading. We need to define its version in libs.versions.toml.
    implementation(libs.coil.compose)

    // DataStore Preferences for storing simple data.
    implementation(libs.androidx.datastore.preferences)

    // Retrofit and Gson for network operations (versions from toml).
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson) // It's good practice to add Gson separately as well.

    // Kotlin Coroutines for asynchronous programming (versions from toml).
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Maps Compose dependencies (versions from toml).
    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.maps)
    implementation(libs.google.places) // Google Places SDK for location-based services.
    implementation(libs.play.services.location) // Google Play Services Location (alias from libs.versions.toml).

    // OkHttp for networking (versions managed by BOM).
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)


    // Testing dependencies (currently disabled).
    // testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.test.ext.junit)
    // androidTestImplementation(libs.androidx.espresso.core)
    // androidTestImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation(libs.androidx.ui.test.manifest)
}
