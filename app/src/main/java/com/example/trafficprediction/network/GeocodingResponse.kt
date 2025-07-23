package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// This is our general Geocoding response structure.
data class GeocodingResponse(
    @SerializedName("results")
    val results: List<GeocodingResult>?,
    @SerializedName("status")
    val status: String?
)

// Represents each individual result from the Geocoding API.
data class GeocodingResult(
    @SerializedName("formatted_address")
    val formattedAddress: String?, // The human-readable address.
    @SerializedName("geometry")
    val geometry: Geometry?
)

// Contains geometry information, including the location.
data class Geometry(
    @SerializedName("location")
    val location: Location?
)

// Represents latitude and longitude.
data class Location(
    @SerializedName("lat")
    val latitude: Double?,
    @SerializedName("lng")
    val longitude: Double?
)
