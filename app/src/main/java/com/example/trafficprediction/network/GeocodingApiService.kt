package com.example.trafficprediction.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    @GET("maps/api/geocode/json") // This is the Google Geocoding API endpoint.
    suspend fun getCoordinates(
        @Query("address") address: String,
        @Query("key") apiKey: String // We send the API Key as a query parameter.
    ): Response<GeocodingResponse>

    @GET("maps/api/directions/json") // This is the Google Directions API endpoint.
    suspend fun getDirections(
        @Query("origin") origin: String, // Can be "latitude,longitude" or an address/place_id.
        @Query("destination") destination: String, // Can be "latitude,longitude" or an address/place_id.
        @Query("key") apiKey: String,
        @Query("departure_time") departureTime: String = "now", // For real-time traffic.
        @Query("traffic_model") trafficModel: String = "best_guess" // Traffic prediction model.
        // Alternatives include "pessimistic", "optimistic".
    ): Response<DirectionsResponse> // Our new Response data class.

    @GET("maps/api/place/nearbysearch/json")
    suspend fun findNearbyPlaces(
        @Query("location") location: String, // Format: "latitude,longitude".
        @Query("radius") radius: Int, // Radius in meters.
        @Query("type") type: String, // Example: "gas_station", "restaurant".
        @Query("key") apiKey: String
    ): Response<PlacesNearbySearchResponse> // Our new Response data class for nearby places.
}
