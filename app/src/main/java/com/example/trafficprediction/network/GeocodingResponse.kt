package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// Genel Geocoding yanıtı
data class GeocodingResponse(
    @SerializedName("results")
    val results: List<GeocodingResult>?,
    @SerializedName("status")
    val status: String?
)

// Her bir sonuç
data class GeocodingResult(
    @SerializedName("formatted_address") // Eklendi
    val formattedAddress: String?,       // Eklendi
    @SerializedName("geometry")
    val geometry: Geometry?
)

// Geometri bilgisi (konumu içerir)
data class Geometry(
    @SerializedName("location")
    val location: Location?
)

// Enlem ve Boylam
data class Location(
    @SerializedName("lat")
    val latitude: Double?,
    @SerializedName("lng")
    val longitude: Double?
)
