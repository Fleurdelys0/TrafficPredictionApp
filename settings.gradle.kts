// settings.gradle.kts

// Bu blok, Gradle'ın eklentileri ve onların versiyonlarını nasıl yöneteceğini tanımlar.
pluginManagement {
    // 1. Eklentilerin aranacağı depolar (repositories)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // 2. Kullanılacak ana eklentilerin ID'leri ve versiyonları
    //    Burada tanımlanan versiyonlar, modül seviyesindeki build.gradle.kts'lerde
    //    versiyon belirtmeden sadece ID ile plugin eklemeyi sağlar.
    plugins {
        // Android uygulama eklentisi ve versiyonu
        id("com.android.application") version "8.2.2" apply false // Kendi AGP versiyonunu yaz
        // Kotlin Android eklentisi ve versiyonu
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false // Kendi Kotlin versiyonunu yaz
        // Compose Compiler eklentisi ve versiyonu (Kotlin versiyonuna uygun olmalı)
        id("org.jetbrains.kotlin.plugin.compose") apply false // Kotlin 1.9.22 için uygun versiyon (veya seninkine uygun olan)
        // Google Services eklentisi (Eğer kök projede tanımlanıyorsa)
        // Genellikle burada tanımlanır, versiyonu kontrol et
        id("com.google.gms.google-services") version "4.4.2" apply false // Kendi versiyonunu yaz
    }
}

// Bu blok, projenin bağımlılıklarının (kütüphanelerin) nereden bulunacağını tanımlar.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Başka depolar (örn. JitPack) gerekiyorsa buraya eklenebilir
    }
}

rootProject.name = "TrafficPrediction" // Proje adını kontrol et
include(":app")                  // Uygulama modülünü dahil et