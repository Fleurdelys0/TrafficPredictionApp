package com.example.trafficprediction.data

import com.google.firebase.Timestamp // Firestore zaman damgası için
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp // Sunucu zamanı için

// Firestore'a kaydedilecek tahmin logu yapısı
data class TrafficPredictionLog(
    @DocumentId val logId: String? = null,
    val userId: String? = null,
    val startAddress: String = "",
    val endAddress: String = "",
    val requestedTime: String = "",
    val requestedDayType: String = "",
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val predictedSpeed: Double? = null, // Bu, predictedSpeedKmh olabilir, ViewModel'de buna göre ayarlanacak
    val estimatedCondition: String? = "",
    val segmentDistanceKm: Double? = null, // YENİ
    val estimatedTravelTimeMinutes: Double? = null, // YENİ
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    // Firestore'un data class'ı düzgün işlemesi için boş constructor gerekebilir.
    constructor() : this(
        logId = null,
        userId = null,
        startAddress = "",
        endAddress = "",
        requestedTime = "",
        requestedDayType = "",
        startLat = null,
        startLng = null,
        endLat = null,
        endLng = null,
        predictedSpeed = null,
        estimatedCondition = null, // estimatedCondition String? olduğu için null olabilir
        segmentDistanceKm = null, // YENİ
        estimatedTravelTimeMinutes = null, // YENİ
        timestamp = null
    )
}
