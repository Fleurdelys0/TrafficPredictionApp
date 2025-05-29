package com.example.trafficprediction

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import com.google.android.libraries.places.api.Places // Places SDK import
import com.example.trafficprediction.R // R importu (API anahtarı için)

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firestore için diskte kalıcılığı etkinleştir
        try {
            val db = Firebase.firestore
            val settings = firestoreSettings {
                isPersistenceEnabled = true
                // Opsiyonel: Önbellek boyutunu ayarlayabilirsiniz.
                // CACHE_SIZE_UNLIMITED, sınırsız önbellek boyutu anlamına gelir.
                // Alternatif olarak belirli bir byte değeri de verebilirsiniz (örn: 100 * 1024 * 1024 for 100MB).
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }
            db.firestoreSettings = settings
            android.util.Log.d("MainApplication", "Firestore persistence enabled.")
        } catch (e: Exception) {
            android.util.Log.e("MainApplication", "Error enabling Firestore persistence", e)
        }

        // Google Places SDK'sını initialize et
        try {
            val googleMapsApiKey = getString(R.string.google_maps_key)
            if (googleMapsApiKey.isNotEmpty()) {
                if (!Places.isInitialized()) {
                    Places.initialize(applicationContext, googleMapsApiKey)
                    android.util.Log.d("MainApplication", "Google Places SDK initialized.")
                }
            } else {
                android.util.Log.e("MainApplication", "Google Maps API Key is empty. Places SDK cannot be initialized.")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainApplication", "Error initializing Google Places SDK", e)
        }
    }
}
