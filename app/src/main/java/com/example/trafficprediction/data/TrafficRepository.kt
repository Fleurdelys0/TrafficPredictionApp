package com.example.trafficprediction.data

import android.content.Context
import android.util.Log
import com.example.trafficprediction.R // R sınıfını import et
import com.example.trafficprediction.network.* // Tüm network sınıflarını import et (Location, PredictionResponse, API Servisleri)
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.places.api.Places // Places SDK
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response

// Repository artık Context'e ihtiyaç duyacak
class TrafficRepository(
    private val context: Context, // Context parametresi
    private val trafficApiService: TrafficApiService = TrafficApiInstance.api,
    private val geocodingApiService: GeocodingApiService = GeocodingApiInstance.api,
    private val weatherApiService: WeatherApiService = WeatherApiInstance.api,
    // Firestore instance'ını al
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val placesClient: PlacesClient // YENİ: PlacesClient
) {

    // PlacesClient'ı constructor'da initialize etmek yerine, ApplicationContext ile burada oluşturabiliriz.
    // Ancak, ApplicationContext'i ViewModel'den almak ve Repository'ye DI ile sağlamak daha iyi bir pratik olabilir.
    // Şimdilik constructor'a ekledik, ViewModel'de initialize edip buraya geçeceğiz.
    // Alternatif: init bloğunda context ile initialize edilebilir.
    // init {
    // if (!Places.isInitialized()) {
    // Places.initialize(context, getGoogleMapsApiKey())
    // }
    // placesClient = Places.createClient(context)
    // }


    // Rota trafik bilgisini tutacak basit data class
    data class RouteTrafficInfo(
        val routeName: String,
        val durationInSeconds: Int?, // Trafiksiz süre (saniye)
        val durationInTrafficInSeconds: Int?, // Trafikli süre (saniye)
        val summary: String? // API'den gelen özet
    )

    // API Anahtarlarını Context üzerinden, gerektiğinde oku
    private fun getCloudFunctionApiKey(): String {
        return try {
            val key = context.getString(R.string.cloud_function_key)
            Log.d("TrafficRepository", "Cloud Function API Key read from resources: ${key.take(5)}...") // Anahtarın tamamını loglama
            if (key.isEmpty()) Log.w("TrafficRepository", "Cloud Function API Key is EMPTY in resources!")
            key
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read cloud_function_key from resources", e)
            "" // Hata durumunda boş döndür
        }
    }

    private fun getGoogleMapsApiKey(): String {
        return try {
            val key = context.getString(R.string.google_maps_key)
            Log.d("TrafficRepository", "Google Maps API Key read from resources: ${key.take(5)}...") // Anahtarın tamamını loglama
            if (key.isEmpty()) Log.w("TrafficRepository", "Google Maps API Key is EMPTY in resources!")
            key
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read google_maps_key from resources", e)
            "" // Hata durumunda boş döndür
        }
    }

    // Trafik Tahmini Alma Fonksiyonu
    suspend fun getTrafficPrediction(
        fromX: Double, fromY: Double, toX: Double, toY: Double,
        time: Int, isWeekday: Int
    ): Result<PredictionResponse> {
        val apiKey = getCloudFunctionApiKey() // Anahtarı al
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Cloud Function API Key IS EMPTY, cannot make prediction request.")
            return Result.failure(Exception("Cloud Function API Key not found in resources"))
        }
        Log.d("TrafficRepository", "Making traffic prediction request...")
        return withContext(Dispatchers.IO) {
            try {
                // API servisine yeni parametrelerle istek gönder
                val response = trafficApiService.getTrafficPrediction(
                    apiKey, fromX, fromY, toX, toY, time, isWeekday
                )
                Log.d("TrafficRepository", "Traffic API response code: ${response.code()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("TrafficRepository", "Traffic API Success: $it")
                        Result.success(it)
                    } ?: Result.failure(Exception("Traffic API response body is null"))
                } else {
                    Log.e("TrafficRepository", "Traffic API Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Traffic API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Traffic API network error", e)
                Result.failure(e)
            } // Bu kapanış parantezi bir önceki kodda eksikti, eklendi.
        }
    }

    // Belirli bir rota için trafik bilgisini alma fonksiyonu
    suspend fun getRouteTrafficInfo(
        origin: String,          // Başlangıç (adres veya "lat,lng")
        destination: String,     // Bitiş (adres veya "lat,lng")
        routeName: String        // Rota için bir isim (UI'da göstermek için)
    ): Result<RouteTrafficInfo> {
        val apiKey = getGoogleMapsApiKey() // Directions API de genellikle Maps API key'i kullanır
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Google Maps API Key IS EMPTY, cannot make Directions request.")
            return Result.failure(Exception("Google Maps API Key not found"))
        }
        Log.d("TrafficRepository", "Fetching directions for route: $routeName ($origin to $destination)")
        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.getDirections( // GeocodingApiService üzerinden çağırıyoruz
                    origin = origin,
                    destination = destination,
                    apiKey = apiKey
                    // departureTime = "now" ve trafficModel = "best_guess" varsayılan olarak kullanılacak
                )
                Log.d("TrafficRepository", "Directions API response code: ${response.code()} for route $routeName")
                if (response.isSuccessful && response.body() != null) {
                    val directionsResponse = response.body()!!
                    if (directionsResponse.status == "OK" && directionsResponse.routes?.isNotEmpty() == true && directionsResponse.routes[0].legs?.isNotEmpty() == true) {
                        val leg = directionsResponse.routes[0].legs!![0]
                        val info = RouteTrafficInfo(
                            routeName = routeName,
                            durationInSeconds = leg.duration?.value,
                            durationInTrafficInSeconds = leg.durationInTraffic?.value,
                            summary = directionsResponse.routes[0].summary
                        )
                        Log.d("TrafficRepository", "Directions API Success for $routeName: $info")
                        Result.success(info)
                    } else {
                        Log.w("TrafficRepository", "Directions API status not OK or no routes/legs for $routeName. Status: ${directionsResponse.status}")
                        Result.failure(Exception("Could not retrieve directions for $routeName. Status: ${directionsResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e("TrafficRepository", "Directions API Error for $routeName: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Directions API Error for $routeName: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Directions API network error for $routeName", e)
                Result.failure(e)
            }
        }
    }

    // YENİ FONKSİYON: Rota Polyline'ını Alma
    suspend fun getRoutePolyline(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): Result<String?> {
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Google Maps API Key IS EMPTY, cannot make Directions request for polyline.")
            return Result.failure(Exception("Google Maps API Key not found"))
        }
        val originStr = "$originLat,$originLng"
        val destinationStr = "$destinationLat,$destinationLng"
        Log.d("TrafficRepository", "Fetching route polyline for: $originStr to $destinationStr")

        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.getDirections(
                    origin = originStr,
                    destination = destinationStr,
                    apiKey = apiKey
                )
                Log.d("TrafficRepository", "Directions API (polyline) response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val directionsResponse = response.body()!!
                    if (directionsResponse.status == "OK" && directionsResponse.routes?.isNotEmpty() == true) {
                        val polylinePoints = directionsResponse.routes[0].overviewPolyline?.points
                        if (polylinePoints != null) {
                            Log.d("TrafficRepository", "Directions API (polyline) Success. Points: ${polylinePoints.take(30)}...")
                            Result.success(polylinePoints)
                        } else {
                            Log.w("TrafficRepository", "Directions API (polyline) OK but overview_polyline points are null.")
                            Result.failure(Exception("Route polyline not found in response."))
                        }
                    } else {
                        Log.w("TrafficRepository", "Directions API (polyline) status not OK or no routes. Status: ${directionsResponse.status}")
                        Result.failure(Exception("Could not retrieve route polyline. Status: ${directionsResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e("TrafficRepository", "Directions API (polyline) Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Directions API (polyline) Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Directions API (polyline) network error", e)
                Result.failure(e)
            }
        }
    }
    // ---

    // Adresten Koordinat Alma Fonksiyonu (Geocoding Status Kontrolü İyileştirildi)
    suspend fun getCoordinatesFromAddress(address: String): Result<Location> {
        val apiKey = getGoogleMapsApiKey() // Anahtarı al
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Google Maps API Key IS EMPTY, cannot make Geocoding request.")
            return Result.failure(Exception("Google Maps API Key not found in resources"))
        }
        Log.d("TrafficRepository", "Fetching coordinates for address: '$address'")
        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.getCoordinates(address, apiKey)
                Log.d("TrafficRepository", "Geocoding API response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val geocodingResponse = response.body()!!
                    Log.d("TrafficRepository", "Geocoding API Status: ${geocodingResponse.status}")
                    // Status kontrolü
                    when (geocodingResponse.status) {
                        "OK" -> {
                            if (geocodingResponse.results?.isNotEmpty() == true) {
                                val location = geocodingResponse.results[0].geometry?.location
                                if (location?.latitude != null && location.longitude != null) {
                                    Log.d("TrafficRepository", "Coordinates found: Lat=${location.latitude}, Lng=${location.longitude}")
                                    Result.success(location)
                                } else {
                                    Log.w("TrafficRepository", "Geocoding OK but location data missing for '$address'")
                                    Result.failure(Exception("Coordinates not found in result for '$address'"))
                                }
                            } else {
                                Log.w("TrafficRepository", "Geocoding OK but ZERO_RESULTS for '$address'")
                                Result.failure(Exception("Address not found: '$address'. Please be more specific."))
                            }
                        }
                        "ZERO_RESULTS" -> {
                            Log.w("TrafficRepository", "Geocoding returned ZERO_RESULTS for '$address'")
                            Result.failure(Exception("Address not found: '$address'. Please be more specific."))
                        }
                        else -> {
                            Log.w("TrafficRepository", "Geocoding API status not OK for '$address'. Status: ${geocodingResponse.status}")
                            Result.failure(Exception("Geocoding failed for '$address'. Status: ${geocodingResponse.status ?: "Unknown"}"))
                        }
                    }
                } else {
                    Log.e("TrafficRepository", "Geocoding API Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Geocoding API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Geocoding network error", e)
                Result.failure(e)
            }
        }
    }

    // Koordinatlardan Adres Alma Fonksiyonu (Reverse Geocoding)
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): Result<String> {
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Google Maps API Key IS EMPTY, cannot make Reverse Geocoding request.")
            return Result.failure(Exception("Google Maps API Key not found in resources"))
        }
        val latLngString = "$latitude,$longitude"
        Log.d("TrafficRepository", "Fetching address for coordinates: '$latLngString'")
        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.getCoordinates(latLngString, apiKey) 
                Log.d("TrafficRepository", "Reverse Geocoding API response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val geocodingResponse = response.body()!!
                    Log.d("TrafficRepository", "Reverse Geocoding API Status: ${geocodingResponse.status}")
                    when (geocodingResponse.status) {
                        "OK" -> {
                            if (geocodingResponse.results?.isNotEmpty() == true) {
                                val formattedAddress = geocodingResponse.results[0].formattedAddress
                                if (!formattedAddress.isNullOrBlank()) {
                                    Log.d("TrafficRepository", "Address found: $formattedAddress")
                                    Result.success(formattedAddress)
                                } else {
                                    Log.w("TrafficRepository", "Reverse Geocoding OK but formatted_address missing for '$latLngString'")
                                    Result.failure(Exception("Address not found in result for '$latLngString'"))
                                }
                            } else {
                                Log.w("TrafficRepository", "Reverse Geocoding OK but ZERO_RESULTS for '$latLngString'")
                                Result.failure(Exception("No address found for '$latLngString'."))
                            }
                        }
                        "ZERO_RESULTS" -> {
                            Log.w("TrafficRepository", "Reverse Geocoding returned ZERO_RESULTS for '$latLngString'")
                            Result.failure(Exception("No address found for '$latLngString'."))
                        }
                        else -> {
                            Log.w("TrafficRepository", "Reverse Geocoding API status not OK for '$latLngString'. Status: ${geocodingResponse.status}")
                            Result.failure(Exception("Reverse geocoding failed for '$latLngString'. Status: ${geocodingResponse.status ?: "Unknown"}"))
                        }
                    }
                } else {
                    Log.e("TrafficRepository", "Reverse Geocoding API Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Reverse Geocoding API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Reverse Geocoding network error", e)
                Result.failure(e)
            }
        }
    }

    // Tahmin Logunu Kaydetme Fonksiyonu
    suspend fun savePredictionLog(logEntry: TrafficPredictionLog): Result<Unit> {
        Log.d("TrafficRepository", "Attempting to save prediction log to Firestore: $logEntry")
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("predictions")
                    .add(logEntry)
                    .await()
                Log.d("TrafficRepository", "Prediction log saved successfully.")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Error saving prediction log to Firestore", e)
                Result.failure(e)
            }
        }
    }

    // Tahmin Geçmişini Alma Fonksiyonu
    suspend fun getPredictionHistory(): Result<List<TrafficPredictionLog>> {
        Log.d("TrafficRepository", "Fetching prediction history from Firestore...")
        return withContext(Dispatchers.IO) {
            try {
                val userId = Firebase.auth.currentUser?.uid 
                if (userId == null) {
                    Log.w("TrafficRepository", "User not logged in, cannot fetch history.")
                    return@withContext Result.success(emptyList()) 
                }

                val querySnapshot = firestore.collection("predictions")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                val historyList = querySnapshot.toObjects(TrafficPredictionLog::class.java)
                Log.d("TrafficRepository", "Fetched ${historyList.size} prediction logs.")
                Result.success(historyList)
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Error fetching prediction history", e)
                Result.failure(e)
            }
        }
    }

    // Belirli bir tahmin logunu silme fonksiyonu
    suspend fun deletePredictionLog(logId: String): Result<Unit> {
        Log.d("TrafficRepository", "Attempting to delete prediction log with ID: $logId")
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("predictions").document(logId)
                    .delete()
                    .await()
                Log.d("TrafficRepository", "Prediction log deleted successfully: $logId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Error deleting prediction log $logId", e)
                Result.failure(e)
            }
        }
    }

    // Mevcut Hava Durumunu Alma Fonksiyonu
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): Result<CurrentWeatherResponse> {
        val apiKey = try {
            context.getString(R.string.openweathermap_api_key)
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read openweathermap_api_key from resources", e)
            return Result.failure(Exception("OpenWeatherMap API Key not found in resources"))
        }

        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "OpenWeatherMap API Key IS EMPTY, cannot make weather request.")
            return Result.failure(Exception("OpenWeatherMap API Key is empty"))
        }
        Log.d("TrafficRepository", "Making current weather request for Lat: $latitude, Lon: $longitude")
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherApiService.getCurrentWeather(latitude, longitude, apiKey)
                Log.d("TrafficRepository", "Weather API response code: ${response.code()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("TrafficRepository", "Weather API Success: ${it.weather?.firstOrNull()?.main}")
                        Result.success(it)
                    } ?: Result.failure(Exception("Weather API response body is null"))
                } else {
                    Log.e("TrafficRepository", "Weather API Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Weather API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Weather API network error", e)
                Result.failure(e)
            }
        }
    }

    // Yakındaki Yerleri Bulma Fonksiyonu (Örnek: Benzin İstasyonları)
    suspend fun findNearbyPlaces(
        latitude: Double,
        longitude: Double,
        placeType: String, // Örn: "gas_station", "restaurant"
        radiusMeters: Int = 5000 // Arama yarıçapı (metre)
    ): Result<List<PlaceResult>> { // Dönüş tipi PlaceResult olarak güncellendi
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e("TrafficRepository", "Google Maps API Key IS EMPTY, cannot make Nearby Search request.")
            return Result.failure(Exception("Google Maps API Key not found"))
        }

        val locationString = "$latitude,$longitude"
        Log.d("TrafficRepository", "Fetching nearby places of type '$placeType' around $locationString with radius $radiusMeters")

        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.findNearbyPlaces(
                    location = locationString,
                    radius = radiusMeters,
                    type = placeType,
                    apiKey = apiKey
                )
                Log.d("TrafficRepository", "Places Nearby Search API response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val nearbySearchResponse = response.body()!!
                    if (nearbySearchResponse.status == "OK") {
                        Log.d("TrafficRepository", "Found ${nearbySearchResponse.results?.size ?: 0} places of type $placeType.")
                        Result.success(nearbySearchResponse.results ?: emptyList())
                    } else {
                        Log.w("TrafficRepository", "Places Nearby Search API status not OK: ${nearbySearchResponse.status}")
                        Result.failure(Exception("Could not retrieve nearby places. Status: ${nearbySearchResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e("TrafficRepository", "Places Nearby Search API Error: ${response.code()} ${response.message()} - Body: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Places Nearby Search API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Places Nearby Search API network error", e)
                Result.failure(e)
            }
        }
    }
}
