package com.example.trafficprediction

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trafficprediction.network.WeatherApiInstance // We use WeatherApiInstance instead of RetrofitInstance here.
import com.example.trafficprediction.network.WeatherApiService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WeatherCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "WeatherCheckWorker"
    private val weatherApiService: WeatherApiService =
        WeatherApiInstance.api // We get the API service from WeatherApiInstance.

    companion object {
        const val WEATHER_ALERTS_CHANNEL_ID = "weather_alerts_channel"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "WeatherCheckWorker started.")

        // 1. Get Location
        val location = getLastKnownLocation()
        if (location == null) {
            Log.e(TAG, "Location could not be retrieved. Worker is terminating.")
            // If location permission is not granted or location cannot be retrieved, we terminate without sending a notification.
            // Optionally, we could send a notification to the user to check location settings.
            return Result.failure()
        }
        Log.d(TAG, "Location retrieved: Lat: ${location.latitude}, Lon: ${location.longitude}")

        // 2. Make a Request to the Weather API
        try {
            val apiKey = applicationContext.getString(R.string.openweathermap_api_key)
            val response = weatherApiService.getCurrentWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                apiKey = apiKey,
                units = "metric" // We want the temperature in Celsius.
            )

            if (response.isSuccessful && response.body() != null) {
                val weatherData = response.body()!!
                Log.d(
                    TAG,
                    "Weather data received: ${weatherData.weather?.firstOrNull()?.description}"
                )

                // 3. Check for Bad Weather Conditions
                // According to OpenWeatherMap API weather codes:
                // 2xx: Thunderstorm
                // 3xx: Drizzle
                // 5xx: Rain
                // 6xx: Snow
                // 7xx: Atmosphere (Mist, Smoke, Haze, etc.)
                // 800: Clear
                // 80x: Clouds
                val weatherConditionId = weatherData.weather?.firstOrNull()?.id ?: 0
                val weatherDescription =
                    weatherData.weather?.firstOrNull()?.description ?: "Unknown"
                val temperature = weatherData.main?.temp

                // Example bad weather conditions: Storm, Heavy Rain, Snow.
                // We can customize these conditions based on our project's needs.
                val isBadWeather = when (weatherConditionId) {
                    in 200..299 -> true // Thunderstorm
                    in 502..504 -> true // Heavy intensity rain
                    in 521..531 -> true // Shower rain (can be heavy)
                    in 601..602 -> true // Snow / Heavy Snow
                    in 611..622 -> true // Sleet / Shower Snow (can be heavy)
                    // We can add other potential "bad" conditions (e.g., strong wind, fog, etc.)
                    // e.g., 741 (fog), 781 (tornado)
                    else -> false
                }

                if (isBadWeather) {
                    Log.d(TAG, "Bad weather condition detected: $weatherDescription")
                    sendWeatherNotification(
                        "Weather Alert",
                        "Attention! Adverse weather conditions (${
                            weatherDescription.capitalize(
                                Locale.ENGLISH
                            )
                        }, ${temperature}°C) are expected in your area. Be cautious."
                    )
                } else {
                    Log.d(TAG, "Weather condition normal: $weatherDescription")
                }
                return Result.success()
            } else {
                Log.e(TAG, "Weather API error: ${response.code()} - ${response.message()}")
                return Result.retry() // We'll retry in case of an API error.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during weather check", e)
            return Result.retry() // We'll retry if an exception occurs.
        }
    }

    private suspend fun getLastKnownLocation(): android.location.Location? {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted.")
            // If permission is not granted, we return null; it will be handled in doWork.
            return null
        }
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
        return try {
            // We try to get the last known location.
            var location = fusedLocationClient.lastLocation.await()
            if (location == null) {
                // If last known location is null, we could request the current location (this might take longer).
                // Note: Since this worker runs in the background, requesting current location might not always succeed
                // or might exceed the worker's lifetime. Last known location is usually sufficient.
                // location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                Log.d(TAG, "Last known location is null, current location not attempted (worker context).")
            }
            location
        } catch (e: SecurityException) {
            Log.e(TAG, "Konum alınırken güvenlik istisnası.", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Konum alınırken genel hata.", e)
            null
        }
    }

    private fun sendWeatherNotification(title: String, messageBody: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1 /* Request code */, // We use a different request code than MyFirebaseMessagingService.
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, WEATHER_ALERTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // We use the app icon.
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(messageBody)
                ) // For longer text.
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // We assume the channel is created in MainApplication.
        // A check similar to the one in MyFirebaseMessagingService could be added, but it's usually not necessary.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(WEATHER_ALERTS_CHANNEL_ID)
            if (channel == null) { // This is an unexpected situation; the channel should have been created in MainApplication.
                Log.w(TAG, "$WEATHER_ALERTS_CHANNEL_ID channel not found, creating...")
                channel = NotificationChannel(
                    WEATHER_ALERTS_CHANNEL_ID,
                    "Weather Alerts", // We'll also make the channel name English.
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Notifications for sudden weather changes" // And the channel description English.
                }
                notificationManager.createNotificationChannel(channel)
            }
        }


        val notificationId = System.currentTimeMillis().toInt() + 1 // We use a unique ID.
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Weather notification sent: ID $notificationId, Title: $title")
    }
}
