package com.example.trafficprediction.data

import com.google.firebase.Timestamp // For Firestore timestamps.
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp // For server-generated timestamps.

// This data class represents the structure of a prediction log that we'll save to Firestore.
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
    val predictedSpeed: Double? = null, // This could be predictedSpeedKmh; we'll adjust it in the ViewModel accordingly.
    val estimatedCondition: String? = "",
    val segmentDistanceKm: Double? = null,
    val estimatedTravelTimeMinutes: Double? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    // Firestore might need a no-argument constructor to properly handle the data class.
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
        estimatedCondition = null, // estimatedCondition is String?, so it can be null.
        segmentDistanceKm = null,
        estimatedTravelTimeMinutes = null,
        timestamp = null
    )
}
