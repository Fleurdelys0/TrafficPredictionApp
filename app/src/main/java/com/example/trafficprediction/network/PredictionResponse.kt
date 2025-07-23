package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// Data class representing the JSON response from our Cloud Function.
data class PredictionResponse(

    // This part is optional, added to confirm the request.
    @SerializedName("requested_params")
    val requestedParams: Map<String, Any>?, // We can get the incoming parameters as a map.

    @SerializedName("predicted_speed_kmh") // Name updated and made compatible with the Cloud Function.
    val predictedSpeedKmh: Double?,

    @SerializedName("estimated_condition")
    val estimatedCondition: String?, // Density status.

    @SerializedName("segment_distance_km")
    val segmentDistanceKm: Double?,

    @SerializedName("estimated_travel_time_minutes")
    val estimatedTravelTimeMinutes: Double?,

    @SerializedName("source")
    val source: String?
)
