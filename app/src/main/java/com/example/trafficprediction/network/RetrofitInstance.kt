package com.example.trafficprediction.network

// We've REMOVED the BuildConfig import.
// import com.example.trafficprediction.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Interceptor for logging network requests (intended for Debug builds).
// Since we can't use BuildConfig, we can control logging manually
// or leave it always on for now.
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    // level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    // As BuildConfig is not available, let's always log for now (or we can set it to NONE).
    level = HttpLoggingInterceptor.Level.BODY
}

// OkHttpClient with timeout and logging settings.
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout.
    .readTimeout(30, TimeUnit.SECONDS)    // Read timeout.
    .build()


// --- Retrofit Instance for Our Cloud Function ---
object TrafficApiInstance {
    // Our Cloud Function URL (unchanged).
    private const val BASE_URL = "https://get-traffic-prediction-ghuf2bsaaq-uc.a.run.app/"


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // We use the configured OkHttpClient.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // This part remains unchanged.
    val api: TrafficApiService by lazy {
        retrofit.create(TrafficApiService::class.java)
    }
}

// --- Retrofit Instance for Google Geocoding API ---
object GeocodingApiInstance {
    // Google API Base URL (unchanged).
    private const val BASE_URL = "https://maps.googleapis.com/"


    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // We use the configured OkHttpClient.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // This part remains unchanged.
    val api: GeocodingApiService by lazy {
        retrofit.create(GeocodingApiService::class.java)
    }
}

// --- Retrofit Instance for OpenWeatherMap API ---
object WeatherApiInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // We can use the same OkHttpClient.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}
