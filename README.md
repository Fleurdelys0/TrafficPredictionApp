# 🚦 Traffic Prediction App

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="App Logo" width="150"/>
</p>

An intelligent, native Android application designed to predict traffic density using a machine learning model, helping users optimize their travel time and avoid congestion. The app provides real-time traffic forecasts, integrates weather conditions, and offers personalized features through Firebase authentication.

---

## ✨ Features

-   **🔮 Real-Time Traffic Prediction**: Utilizes a Python-based Machine Learning model (RandomForest) hosted on Google Cloud Functions to predict traffic speed and conditions for a given route and time.
-   **🗺️ Interactive Map Interface**: Built with Jetpack Compose and Google Maps SDK, allowing users to select start and end points directly on the map.
-   **👤 User Authentication**: Secure user registration and login system powered by Firebase Authentication.
-   **⭐ Favorite Routes & Notifications**: Users can save their favorite routes (e.g., "Home to Work"). A scheduled Firebase Cloud Function checks for traffic on these routes hourly and sends a push notification via FCM if there's a significant delay.
-   **☀️ Weather Integration**: Fetches and displays current weather conditions from the OpenWeatherMap API, as weather can significantly impact traffic.
-   **📊 Prediction History**: Saves all user predictions to Firestore, allowing them to review their past queries.
-   **🎨 Dynamic & Themed UI**: A modern user interface built entirely with Jetpack Compose, featuring custom fonts, light/dark themes, and Material 3 components.
-   **☁️ Backend on Firebase & Google Cloud**: The entire backend logic, including the ML model, scheduled tasks, and database, is hosted on Firebase and Google Cloud, providing a scalable and serverless architecture.

---

## 🛠️ Tech Stack & Architecture

This project is built with a modern, scalable, and maintainable technology stack, following the MVVM (Model-View-ViewModel) architecture pattern.

### 📱 Frontend (Android App)

-   **Language**: [Kotlin](https://kotlinlang.org/)
-   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
-   **Architecture**: MVVM (Model-View-ViewModel)
-   **Asynchronous Programming**: Kotlin Coroutines
-   **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & [OkHttp 3](https://square.github.io/okhttp/)
-   **Dependency Injection**: Hilt (Implicitly, via ViewModels)
-   **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
-   **Maps**: [Google Maps Compose SDK](https://developers.google.com/maps/documentation/android-sdk/maps-compose) & [Google Places SDK](https://developers.google.com/maps/documentation/places/android-sdk/overview)
-   **Data Persistence**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) for user settings (e.g., theme).

### ☁️ Backend & Cloud

-   **Platform**: [Firebase](https://firebase.google.com/) & [Google Cloud Platform](https://cloud.google.com/)
-   **Serverless Functions**: [Firebase Cloud Functions for Python](https://firebase.google.com/docs/functions/writing-functions) to host the ML model API and scheduled tasks.
-   **Database**: [Cloud Firestore](https://firebase.google.com/docs/firestore) (NoSQL) for storing user data, favorite routes, and prediction history.
-   **Authentication**: [Firebase Authentication](https://firebase.google.com/docs/auth)
-   **Push Notifications**: [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging)
-   **ML Model Hosting**: The `traffic_model.joblib` file is stored in [Google Cloud Storage](https://cloud.google.com/storage) and loaded by the Cloud Function on demand.

### 🤖 Machine Learning

-   **Library**: [Scikit-learn](https://scikit-learn.org/stable/)
-   **Data Manipulation**: [Pandas](https://pandas.pydata.org/)
-   **Model**: The prediction model is a RandomForest model trained on historical traffic data.

---

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

-   Android Studio (Latest stable version recommended)
-   Google Account & Firebase Account
-   Node.js and Firebase CLI for deploying Cloud Functions

### Installation & Setup

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/your-username/TrafficPredictionApp.git](https://github.com/your-username/TrafficPredictionApp.git)
    cd TrafficPredictionApp
    ```

2.  **Firebase Setup:**
    -   Create a new project on the [Firebase Console](https://console.firebase.google.com/).
    -   Add an Android app with the package name `com.example.trafficprediction`.
    -   Download the `google-services.json` file and place it in the `app/` directory.
    -   In the Firebase project, enable **Authentication** (with Email/Password), **Firestore**, and **Storage**.

3.  **API Keys:**
    -   Create a `local.properties` file in the root directory of the project.
    -   Add your API keys to this file. You need keys for Google Maps, OpenWeatherMap, and a custom key for your Cloud Function.
        ```properties
        MAPS_API_KEY="YOUR_Maps_API_KEY"
        CF_API_KEY="CREATE_A_SECURE_RANDOM_STRING_FOR_YOUR_CLOUD_FUNCTION"
        OPEN_WEATHER_MAP_API_KEY="YOUR_OPENWEATHERMAP_API_KEY"
        ```

4.  **Cloud Functions Setup:**
    -   Navigate to the `functions` directory: `cd functions`
    -   Install dependencies: `pip install -r requirements.txt`
    -   Set environment variables for the functions. This can be done via the Google Cloud console or using the Firebase CLI:
        ```sh
        firebase functions:config:set gmaps.key="YOUR_Maps_API_KEY" myapi.key="THE_SAME_CF_API_KEY_YOU_SET_IN_LOCAL_PROPERTIES"
        ```
    -   Deploy the functions:
        ```sh
        firebase deploy --only functions
        ```

5.  **Build and Run the App:**
    -   Open the project in Android Studio.
    -   Let Gradle sync the dependencies.
    -   Build and run the application on an Android device or emulator.

---


# 🚦 Trafik Tahmin Uygulaması

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="App Logo" width="150"/>
</p>

Makine öğrenmesi modeli kullanarak trafik yoğunluğunu tahmin etmek, kullanıcıların seyahat sürelerini optimize etmelerine ve sıkışıklıktan kaçınmalarına yardımcı olmak için tasarlanmış akıllı, yerel bir Android uygulamasıdır. Uygulama, gerçek zamanlı trafik tahminleri sunar, hava durumu koşullarını entegre eder ve Firebase kimlik doğrulaması aracılığıyla kişiselleştirilmiş özellikler sunar.

---

## ✨ Özellikler

-   **🔮 Gerçek Zamanlı Trafik Tahmini**: Belirli bir rota ve zaman için trafik hızını ve durumunu tahmin etmek üzere Google Cloud Functions üzerinde barındırılan Python tabanlı bir Makine Öğrenmesi modelini (RandomForest) kullanır.
-   **🗺️ İnteraktif Harita Arayüzü**: Jetpack Compose ve Google Haritalar SDK'sı ile oluşturulmuş olup, kullanıcıların başlangıç ve bitiş noktalarını doğrudan harita üzerinden seçmelerine olanak tanır.
-   **👤 Kullanıcı Kimlik Doğrulaması**: Firebase Authentication tarafından desteklenen güvenli kullanıcı kayıt ve giriş sistemi.
-   **⭐ Favori Rotalar ve Bildirimler**: Kullanıcılar favori rotalarını ("Evden İşe" gibi) kaydedebilirler. Zamanlanmış bir Firebase Cloud Function, bu rotalardaki trafiği saatlik olarak kontrol eder ve önemli bir gecikme varsa FCM aracılığıyla anlık bildirim gönderir.
-   **☀️ Hava Durumu Entegrasyonu**: Hava durumunun trafiği önemli ölçüde etkileyebileceği için OpenWeatherMap API'sinden mevcut hava koşullarını alır ve gösterir.
-   **📊 Tahmin Geçmişi**: Tüm kullanıcı tahminlerini Firestore'a kaydederek geçmiş sorgularını incelemelerine olanak tanır.
-   **🎨 Dinamik ve Temalı Arayüz**: Tamamen Jetpack Compose ile oluşturulmuş, özel fontlar, aydınlık/karanlık temalar ve Material 3 bileşenleri içeren modern bir kullanıcı arayüzü.
-   **☁️ Firebase & Google Cloud Altyapısı**: ML modeli, zamanlanmış görevler ve veritabanı da dahil olmak üzere tüm arka uç mantığı, ölçeklenebilir ve sunucusuz bir mimari sağlayan Firebase ve Google Cloud üzerinde barındırılmaktadır.

---

## 🛠️ Teknoloji Mimarisi

Bu proje, MVVM (Model-View-ViewModel) mimari modelini izleyen modern, ölçeklenebilir ve bakımı kolay bir teknoloji yığını ile oluşturulmuştur.

### 📱 Ön Yüz (Android Uygulaması)

-   **Dil**: [Kotlin](https://kotlinlang.org/)
-   **UI Kiti**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
-   **Mimari**: MVVM (Model-View-ViewModel)
-   **Asenkron Programlama**: Kotlin Coroutines
-   **Ağ İşlemleri**: [Retrofit 2](https://square.github.io/retrofit/) & [OkHttp 3](https://square.github.io/okhttp/)
-   **Bağımlılık Enjeksiyonu**: Hilt (ViewModel'ler aracılığıyla dolaylı olarak)
-   **Navigasyon**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
-   **Haritalar**: [Google Maps Compose SDK](https://developers.google.com/maps/documentation/android-sdk/maps-compose) & [Google Places SDK](https://developers.google.com/maps/documentation/places/android-sdk/overview)
-   **Veri Saklama**: Kullanıcı ayarları (ör. tema) için [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore).

### ☁️ Arka Yüz & Bulut

-   **Platform**: [Firebase](https://firebase.google.com/) & [Google Cloud Platform](https://cloud.google.com/)
-   **Sunucusuz Fonksiyonlar**: ML model API'sini ve zamanlanmış görevleri barındırmak için [Python için Firebase Cloud Functions](https://firebase.google.com/docs/functions/writing-functions).
-   **Veritabanı**: Kullanıcı verilerini, favori rotaları ve tahmin geçmişini depolamak için [Cloud Firestore](https://firebase.google.com/docs/firestore) (NoSQL).
-   **Kimlik Doğrulama**: [Firebase Authentication](https://firebase.google.com/docs/auth)
-   **Anlık Bildirimler**: [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging)
-   **ML Model Barındırma**: `traffic_model.joblib` dosyası [Google Cloud Storage](https://cloud.google.com/storage) üzerinde saklanır ve Cloud Function tarafından talep üzerine yüklenir.

### 🤖 Makine Öğrenmesi

-   **Kütüphane**: [Scikit-learn](https://scikit-learn.org/stable/)
-   **Veri İşleme**: [Pandas](https://pandas.pydata.org/)
-   **Model**: Tahmin modeli, geçmiş trafik verileri üzerine eğitilmiş bir RandomForest modelidir.

---

## 🚀 Kurulum ve Başlatma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki basit adımları izleyin.

### Ön Gereksinimler

-   Android Studio (En son kararlı sürüm önerilir)
-   Google Hesabı ve Firebase Hesabı
-   Cloud Functions'ı dağıtmak için Node.js ve Firebase CLI

### Kurulum Adımları

1.  **Depoyu klonlayın:**
    ```sh
    git clone [https://github.com/kullanici-adiniz/TrafficPredictionApp.git](https://github.com/kullanici-adiniz/TrafficPredictionApp.git)
    cd TrafficPredictionApp
    ```

2.  **Firebase Kurulumu:**
    -   [Firebase Konsolu](https://console.firebase.google.com/)'nda yeni bir proje oluşturun.
    -   `com.example.trafficprediction` paket adıyla bir Android uygulaması ekleyin.
    -   `google-services.json` dosyasını indirin ve projenin `app/` dizinine yerleştirin.
    -   Firebase projenizde **Authentication** (E-posta/Şifre ile), **Firestore** ve **Storage**'ı etkinleştirin.

3.  **API Anahtarları:**
    -   Projenin kök dizininde `local.properties` adında bir dosya oluşturun.
    -   API anahtarlarınızı bu dosyaya ekleyin. Google Haritalar, OpenWeatherMap ve Cloud Function'ınız için özel bir anahtara ihtiyacınız olacak.
        ```properties
        MAPS_API_KEY="Maps_API_ANAHTARINIZ"
        CF_API_KEY="CLOUD_FUNCTION_ICIN_GÜVENLİ_VE_RASTGELE_BİR_ANAHTAR_OLUŞTURUN"
        OPEN_WEATHER_MAP_API_KEY="OPENWEATHERMAP_API_ANAHTARINIZ"
        ```

4.  **Cloud Functions Kurulumu:**
    -   `functions` dizinine gidin: `cd functions`
    -   Bağımlılıkları yükleyin: `pip install -r requirements.txt`
    -   Fonksiyonlar için ortam değişkenlerini ayarlayın. Bu, Google Cloud konsolu üzerinden veya Firebase CLI ile yapılabilir:
        ```sh
        firebase functions:config:set gmaps.key="Maps_API_ANAHTARINIZ" myapi.key="LOCAL.PROPERTIES'DE_AYARLADIĞINIZ_CF_API_KEY_İLE_AYNI"
        ```
    -   Fonksiyonları dağıtın:
        ```sh
        firebase deploy --only functions
        ```

5.  **Uygulamayı Derleyin ve Çalıştırın:**
    -   Projeyi Android Studio'da açın.
    -   Gradle'ın bağımlılıkları senkronize etmesini bekleyin.
    -   Uygulamayı bir Android cihazda veya emülatörde derleyip çalıştırın.

---
