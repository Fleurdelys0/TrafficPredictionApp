package com.example.trafficprediction.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    @GET("maps/api/geocode/json") // Google Geocoding API endpoint'i
    suspend fun getCoordinates(
        @Query("address") address: String,
        @Query("key") apiKey: String // API Anahtarını query parametresi olarak gönderiyoruz
    ): Response<GeocodingResponse>

    @GET("maps/api/directions/json") // Google Directions API endpoint'i
    suspend fun getDirections(
        @Query("origin") origin: String, // "latitude,longitude" veya adres/place_id
        @Query("destination") destination: String, // "latitude,longitude" veya adres/place_id
        @Query("key") apiKey: String,
        @Query("departure_time") departureTime: String = "now", // Anlık trafik için
        @Query("traffic_model") trafficModel: String = "best_guess" // Trafik tahmin modeli
        // Alternatifler: "pessimistic", "optimistic"
    ): Response<DirectionsResponse> // Yeni Response data class'ı

    @GET("maps/api/place/nearbysearch/json")
    suspend fun findNearbyPlaces(
        @Query("location") location: String, // "latitude,longitude"
        @Query("radius") radius: Int, // metre cinsinden
        @Query("type") type: String, // Örneğin: "gas_station", "restaurant"
        @Query("key") apiKey: String
    ): Response<PlacesNearbySearchResponse> // Yeni Response data class'ı
}
