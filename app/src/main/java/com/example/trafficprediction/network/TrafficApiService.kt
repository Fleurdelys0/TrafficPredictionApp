package com.example.trafficprediction.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrafficApiService {

    // Cloud Function URL'si .run.app olduğu için @GET("/") kullanmıştık
    @GET("/")
    suspend fun getTrafficPrediction(
        @Header("x-api-key") apiKey: String, // API Anahtarı
        // --- YENİ PARAMETRELER ---
        @Query("from_x") fromX: Double,
        @Query("from_y") fromY: Double,
        @Query("to_x") toX: Double,
        @Query("to_y") toY: Double,
        @Query("time") time: Int,           // 8, 14, veya 20
        @Query("is_weekday") isWeekday: Int // 0 veya 1
        // --- ---
    ): Response<PredictionResponse> // Yanıt sınıfı aynı kalır
}