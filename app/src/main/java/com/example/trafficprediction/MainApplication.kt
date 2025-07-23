package com.example.trafficprediction

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.libraries.places.api.Places
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // We're enabling disk persistence for Firestore here.
        try {
            val db = Firebase.firestore
            val settings = firestoreSettings {
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }
            db.firestoreSettings = settings
            Log.d("MainApplication", "Firestore persistence enabled.")
        } catch (e: Exception) {
            Log.e("MainApplication", "Error enabling Firestore persistence", e)
        }

        // We're initializing the Google Places SDK.
        try {
            val googleMapsApiKey = getString(R.string.google_maps_key)
            if (googleMapsApiKey.isNotEmpty()) {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, googleMapsApiKey)
                    Log.d("MainApplication", "Google Places SDK initialized.")
                }
            } else {
                Log.e(
                    "MainApplication",
                    "Google Maps API Key is empty. Places SDK cannot be initialized."
                )
            }
        } catch (e: Exception) {
            Log.e("MainApplication", "Error initializing Google Places SDK", e)
        }

        createNotificationChannels()
        setupPeriodicWeatherChecks()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val weatherChannel = NotificationChannel(
                "weather_alerts_channel",
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for sudden weather changes"
            }

            val trafficChannel = NotificationChannel(
                "traffic_alerts_channel",
                "Traffic Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for traffic on your favorite routes"
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(weatherChannel)
            notificationManager.createNotificationChannel(trafficChannel)
            Log.d("MainApplication", "Notification channels created.")
        }
    }

    private fun setupPeriodicWeatherChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherCheckRequest =
            PeriodicWorkRequestBuilder<WeatherCheckWorker>(24, TimeUnit.HOURS) // We're setting this to run once every 24 hours.
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "weatherCheckWork",
            ExistingPeriodicWorkPolicy.KEEP, // If a work with the same unique name already exists, we keep it and don't enqueue a new one.
            weatherCheckRequest
        )
        Log.d("MainApplication", "Periodic weather check work scheduled.")
    }

    // We're providing a custom configuration for WorkManager (optional, but good practice).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO) // We set the minimum logging level.
            .build()
}
