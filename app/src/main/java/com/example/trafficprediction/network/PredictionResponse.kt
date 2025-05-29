package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// Cloud Function'dan dönen YENİ JSON yanıtını temsil eden veri sınıfı
data class PredictionResponse(

    // İsteği teyit etmek için eklediğimiz kısım (opsiyonel)
    @SerializedName("requested_params")
    val requestedParams: Map<String, Any>?, // Gelen parametreleri map olarak alabiliriz

    @SerializedName("predicted_speed_kmh") // İsim güncellendi ve Cloud Function ile uyumlu hale getirildi
    val predictedSpeedKmh: Double?,

    @SerializedName("estimated_condition")
    val estimatedCondition: String?, // Yoğunluk durumu

    @SerializedName("segment_distance_km") // YENİ
    val segmentDistanceKm: Double?,

    @SerializedName("estimated_travel_time_minutes") // YENİ
    val estimatedTravelTimeMinutes: Double?,

    @SerializedName("source")
    val source: String?
)
