package com.example.trafficprediction.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrafficApiService {

    // Since our Cloud Function URL is a .run.app, we used @GET("/").
    @GET("/")
    suspend fun getTrafficPrediction(
        @Header("x-api-key") apiKey: String, // Our API Key.

        @Query("from_x") fromX: Double,
        @Query("from_y") fromY: Double,
        @Query("to_x") toX: Double,
        @Query("to_y") toY: Double,
        @Query("time") time: Int,           // Can be 8, 14, or 20.
        @Query("is_weekday") isWeekday: Int // Can be 0 or 1.

    ): Response<PredictionResponse> // The response class remains the same.
}
