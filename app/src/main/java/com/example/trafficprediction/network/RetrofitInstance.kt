package com.example.trafficprediction.network

// BuildConfig import'unu KALDIRDIK
// import com.example.trafficprediction.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Ağ isteklerini loglamak için interceptor (Debug build'ler için)
// BuildConfig kullanamadığımız için loglamayı manuel olarak kontrol edebiliriz
// veya şimdilik her zaman açık bırakabiliriz.
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    // level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    // BuildConfig olmadığı için şimdilik hep loglasın (veya NONE yapabilirsiniz)
    level = HttpLoggingInterceptor.Level.BODY
}

// OkHttpClient (Timeout ve loglama ayarları)
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .connectTimeout(30, TimeUnit.SECONDS) // Bağlantı zaman aşımı
    .readTimeout(30, TimeUnit.SECONDS)    // Okuma zaman aşımı
    .build()


// --- Bizim Cloud Function İçin Retrofit Instance ---
object TrafficApiInstance {
    // Cloud Function URL'niz (Değişmedi)
    private const val BASE_URL = "https://get-traffic-prediction-ghuf2bsaaq-uc.a.run.app/"


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // OkHttpClient'ı kullan
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Bu kısım değişmedi
    val api: TrafficApiService by lazy {
        retrofit.create(TrafficApiService::class.java)
    }
}

// --- Google Geocoding API İçin Retrofit Instance ---
object GeocodingApiInstance {
    // Google API Base URL (Değişmedi)
    private const val BASE_URL = "https://maps.googleapis.com/"


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // OkHttpClient'ı kullan
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Bu kısım değişmedi
    val api: GeocodingApiService by lazy {
        retrofit.create(GeocodingApiService::class.java)
    }
}

// --- OpenWeatherMap API İçin Retrofit Instance ---
object WeatherApiInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Aynı OkHttpClient'ı kullanabiliriz
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}
