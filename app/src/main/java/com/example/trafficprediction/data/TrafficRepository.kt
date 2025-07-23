package com.example.trafficprediction.data

import android.content.Context
import android.util.Log
import com.example.trafficprediction.R
import com.example.trafficprediction.network.CurrentWeatherResponse
import com.example.trafficprediction.network.GeocodingApiInstance
import com.example.trafficprediction.network.GeocodingApiService
import com.example.trafficprediction.network.Location
import com.example.trafficprediction.network.PlaceResult
import com.example.trafficprediction.network.PredictionResponse
import com.example.trafficprediction.network.TrafficApiInstance
import com.example.trafficprediction.network.TrafficApiService
import com.example.trafficprediction.network.WeatherApiInstance
import com.example.trafficprediction.network.WeatherApiService
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Our repository now needs Context.
class TrafficRepository(
    private val context: Context, // Context parameter.
    private val trafficApiService: TrafficApiService = TrafficApiInstance.api,
    private val geocodingApiService: GeocodingApiService = GeocodingApiInstance.api,
    private val weatherApiService: WeatherApiService = WeatherApiInstance.api,
    private val firestore: FirebaseFirestore = Firebase.firestore, // Get Firestore instance.
    private val placesClient: PlacesClient // PlacesClient for Google Places API.
) {

    // Instead of initializing PlacesClient in the constructor, we could create it here with ApplicationContext.
    // However, getting ApplicationContext from ViewModel and providing it to Repository via DI might be a better practice.
    // For now, we've added it to the constructor; we'll initialize it in ViewModel and pass it here.
    // Alternative: It can be initialized in an init block with context.
    // init {
    // if (!Places.isInitialized()) {
    // Places.initialize(context, getGoogleMapsApiKey())
    // }
    // placesClient = Places.createClient(context)
    // }


    // Simple data class to hold route traffic information.
    data class RouteTrafficInfo(
        val routeName: String,
        val durationInSeconds: Int?, // Duration without traffic (seconds).
        val durationInTrafficInSeconds: Int?, // Duration with traffic (seconds).
        val summary: String? // Summary from the API.
    )

    // We read API Keys from Context when needed.
    private fun getCloudFunctionApiKey(): String {
        return try {
            val key = context.getString(R.string.cloud_function_key)
            Log.d(
                "TrafficRepository",
                "Cloud Function API Key read from resources: ${key.take(5)}..."
            ) // We don't log the entire key.
            if (key.isEmpty()) Log.w(
                "TrafficRepository",
                "Cloud Function API Key is EMPTY in resources!"
            )
            key
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read cloud_function_key from resources", e)
            "" // Return empty on error.
        }
    }

    private fun getGoogleMapsApiKey(): String {
        return try {
            val key = context.getString(R.string.google_maps_key)
            Log.d(
                "TrafficRepository",
                "Google Maps API Key read from resources: ${key.take(5)}..."
            ) // We don't log the entire key.
            if (key.isEmpty()) Log.w(
                "TrafficRepository",
                "Google Maps API Key is EMPTY in resources!"
            )
            key
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read google_maps_key from resources", e)
            "" // Return empty on error.
        }
    }

    // Function to get traffic prediction.
    suspend fun getTrafficPrediction(
        fromX: Double, fromY: Double, toX: Double, toY: Double,
        time: Int, isWeekday: Int
    ): Result<PredictionResponse> {
        val apiKey = getCloudFunctionApiKey() // Get the key.
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Cloud Function API Key IS EMPTY, cannot make prediction request."
            )
            return Result.failure(Exception("Cloud Function API Key not found in resources"))
        }
        Log.d("TrafficRepository", "Making traffic prediction request...")
        return withContext(Dispatchers.IO) {
            try {
                // We send the request to the API service with new parameters.
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
                    Log.e(
                        "TrafficRepository",
                        "Traffic API Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Traffic API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Traffic API network error", e)
                Result.failure(e)
            }
        }
    }

    // Function to get traffic information for a specific route.
    suspend fun getRouteTrafficInfo(
        origin: String,          // Origin (address or "lat,lng").
        destination: String,     // Destination (address or "lat,lng").
        routeName: String        // A name for the route (to display in UI).
    ): Result<RouteTrafficInfo> {
        val apiKey = getGoogleMapsApiKey() // Directions API usually uses the Maps API key as well.
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Google Maps API Key IS EMPTY, cannot make Directions request."
            )
            return Result.failure(Exception("Google Maps API Key not found"))
        }
        Log.d(
            "TrafficRepository",
            "Fetching directions for route: $routeName ($origin to $destination)"
        )
        return withContext(Dispatchers.IO) {
            try {
                val response =
                    geocodingApiService.getDirections( // We call this via GeocodingApiService.
                        origin = origin,
                        destination = destination,
                        apiKey = apiKey
                        // departureTime = "now" and trafficModel = "best_guess" will be used by default.
                    )
                Log.d(
                    "TrafficRepository",
                    "Directions API response code: ${response.code()} for route $routeName"
                )
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
                        Log.w(
                            "TrafficRepository",
                            "Directions API status not OK or no routes/legs for $routeName. Status: ${directionsResponse.status}"
                        )
                        Result.failure(Exception("Could not retrieve directions for $routeName. Status: ${directionsResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Directions API Error for $routeName: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Directions API Error for $routeName: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Directions API network error for $routeName", e)
                Result.failure(e)
            }
        }
    }

    // Function to get the route polyline.
    suspend fun getRoutePolyline(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): Result<String?> {
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Google Maps API Key IS EMPTY, cannot make Directions request for polyline."
            )
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
                Log.d(
                    "TrafficRepository",
                    "Directions API (polyline) response code: ${response.code()}"
                )
                if (response.isSuccessful && response.body() != null) {
                    val directionsResponse = response.body()!!
                    if (directionsResponse.status == "OK" && directionsResponse.routes?.isNotEmpty() == true) {
                        val polylinePoints = directionsResponse.routes[0].overviewPolyline?.points
                        if (polylinePoints != null) {
                            Log.d(
                                "TrafficRepository",
                                "Directions API (polyline) Success. Points: ${polylinePoints.take(30)}..."
                            )
                            Result.success(polylinePoints)
                        } else {
                            Log.w(
                                "TrafficRepository",
                                "Directions API (polyline) OK but overview_polyline points are null."
                            )
                            Result.failure(Exception("Route polyline not found in response."))
                        }
                    } else {
                        Log.w(
                            "TrafficRepository",
                            "Directions API (polyline) status not OK or no routes. Status: ${directionsResponse.status}"
                        )
                        Result.failure(Exception("Could not retrieve route polyline. Status: ${directionsResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Directions API (polyline) Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Directions API (polyline) Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Directions API (polyline) network error", e)
                Result.failure(e)
            }
        }
    }
    // --- End of Polyline Function ---

    // Function to get coordinates from an address (Geocoding status check improved).
    suspend fun getCoordinatesFromAddress(address: String): Result<Location> {
        val apiKey = getGoogleMapsApiKey() // Get the key.
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Google Maps API Key IS EMPTY, cannot make Geocoding request."
            )
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
                    // We check the status.
                    when (geocodingResponse.status) {
                        "OK" -> {
                            if (geocodingResponse.results?.isNotEmpty() == true) {
                                val location = geocodingResponse.results[0].geometry?.location
                                if (location?.latitude != null && location.longitude != null) {
                                    Log.d(
                                        "TrafficRepository",
                                        "Coordinates found: Lat=${location.latitude}, Lng=${location.longitude}"
                                    )
                                    Result.success(location)
                                } else {
                                    Log.w(
                                        "TrafficRepository",
                                        "Geocoding OK but location data missing for '$address'"
                                    )
                                    Result.failure(Exception("Coordinates not found in result for '$address'"))
                                }
                            } else {
                                Log.w(
                                    "TrafficRepository",
                                    "Geocoding OK but ZERO_RESULTS for '$address'"
                                )
                                Result.failure(Exception("Address not found: '$address'. Please be more specific."))
                            }
                        }

                        "ZERO_RESULTS" -> {
                            Log.w(
                                "TrafficRepository",
                                "Geocoding returned ZERO_RESULTS for '$address'"
                            )
                            Result.failure(Exception("Address not found: '$address'. Please be more specific."))
                        }

                        else -> {
                            Log.w(
                                "TrafficRepository",
                                "Geocoding API status not OK for '$address'. Status: ${geocodingResponse.status}"
                            )
                            Result.failure(Exception("Geocoding failed for '$address'. Status: ${geocodingResponse.status ?: "Unknown"}"))
                        }
                    }
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Geocoding API Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Geocoding API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Geocoding network error", e)
                Result.failure(e)
            }
        }
    }

    // Function to get address from coordinates (Reverse Geocoding).
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): Result<String> {
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Google Maps API Key IS EMPTY, cannot make Reverse Geocoding request."
            )
            return Result.failure(Exception("Google Maps API Key not found in resources"))
        }
        val latLngString = "$latitude,$longitude"
        Log.d("TrafficRepository", "Fetching address for coordinates: '$latLngString'")
        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.getCoordinates(latLngString, apiKey)
                Log.d(
                    "TrafficRepository",
                    "Reverse Geocoding API response code: ${response.code()}"
                )
                if (response.isSuccessful && response.body() != null) {
                    val geocodingResponse = response.body()!!
                    Log.d(
                        "TrafficRepository",
                        "Reverse Geocoding API Status: ${geocodingResponse.status}"
                    )
                    when (geocodingResponse.status) {
                        "OK" -> {
                            if (geocodingResponse.results?.isNotEmpty() == true) {
                                val formattedAddress = geocodingResponse.results[0].formattedAddress
                                if (!formattedAddress.isNullOrBlank()) {
                                    Log.d("TrafficRepository", "Address found: $formattedAddress")
                                    Result.success(formattedAddress)
                                } else {
                                    Log.w(
                                        "TrafficRepository",
                                        "Reverse Geocoding OK but formatted_address missing for '$latLngString'"
                                    )
                                    Result.failure(Exception("Address not found in result for '$latLngString'"))
                                }
                            } else {
                                Log.w(
                                    "TrafficRepository",
                                    "Reverse Geocoding OK but ZERO_RESULTS for '$latLngString'"
                                )
                                Result.failure(Exception("No address found for '$latLngString'."))
                            }
                        }

                        "ZERO_RESULTS" -> {
                            Log.w(
                                "TrafficRepository",
                                "Reverse Geocoding returned ZERO_RESULTS for '$latLngString'"
                            )
                            Result.failure(Exception("No address found for '$latLngString'."))
                        }

                        else -> {
                            Log.w(
                                "TrafficRepository",
                                "Reverse Geocoding API status not OK for '$latLngString'. Status: ${geocodingResponse.status}"
                            )
                            Result.failure(Exception("Reverse geocoding failed for '$latLngString'. Status: ${geocodingResponse.status ?: "Unknown"}"))
                        }
                    }
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Reverse Geocoding API Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Reverse Geocoding API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Reverse Geocoding network error", e)
                Result.failure(e)
            }
        }
    }

    // Function to save a prediction log.
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

    // Function to get prediction history.
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

    // Function to delete a specific prediction log.
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

    // Function to get the current weather.
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double
    ): Result<CurrentWeatherResponse> {
        val apiKey = try {
            context.getString(R.string.openweathermap_api_key)
        } catch (e: Exception) {
            Log.e("TrafficRepository", "Could not read openweathermap_api_key from resources", e)
            return Result.failure(Exception("OpenWeatherMap API Key not found in resources"))
        }

        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "OpenWeatherMap API Key IS EMPTY, cannot make weather request."
            )
            return Result.failure(Exception("OpenWeatherMap API Key is empty"))
        }
        Log.d(
            "TrafficRepository",
            "Making current weather request for Lat: $latitude, Lon: $longitude"
        )
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherApiService.getCurrentWeather(latitude, longitude, apiKey)
                Log.d("TrafficRepository", "Weather API response code: ${response.code()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d(
                            "TrafficRepository",
                            "Weather API Success: ${it.weather?.firstOrNull()?.main}"
                        )
                        Result.success(it)
                    } ?: Result.failure(Exception("Weather API response body is null"))
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Weather API Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Weather API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Weather API network error", e)
                Result.failure(e)
            }
        }
    }

    // Function to find nearby places (Example: Gas Stations).
    suspend fun findNearbyPlaces(
        latitude: Double,
        longitude: Double,
        placeType: String, // e.g., "gas_station", "restaurant"
        radiusMeters: Int = 5000 // Search radius in meters.
    ): Result<List<PlaceResult>> { // Return type updated to PlaceResult.
        val apiKey = getGoogleMapsApiKey()
        if (apiKey.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "Google Maps API Key IS EMPTY, cannot make Nearby Search request."
            )
            return Result.failure(Exception("Google Maps API Key not found"))
        }

        val locationString = "$latitude,$longitude"
        Log.d(
            "TrafficRepository",
            "Fetching nearby places of type '$placeType' around $locationString with radius $radiusMeters"
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = geocodingApiService.findNearbyPlaces(
                    location = locationString,
                    radius = radiusMeters,
                    type = placeType,
                    apiKey = apiKey
                )
                Log.d(
                    "TrafficRepository",
                    "Places Nearby Search API response code: ${response.code()}"
                )
                if (response.isSuccessful && response.body() != null) {
                    val nearbySearchResponse = response.body()!!
                    if (nearbySearchResponse.status == "OK") {
                        Log.d(
                            "TrafficRepository",
                            "Found ${nearbySearchResponse.results?.size ?: 0} places of type $placeType."
                        )
                        Result.success(nearbySearchResponse.results ?: emptyList())
                    } else {
                        Log.w(
                            "TrafficRepository",
                            "Places Nearby Search API status not OK: ${nearbySearchResponse.status}"
                        )
                        Result.failure(Exception("Could not retrieve nearby places. Status: ${nearbySearchResponse.status ?: "Unknown"}"))
                    }
                } else {
                    Log.e(
                        "TrafficRepository",
                        "Places Nearby Search API Error: ${response.code()} ${response.message()} - Body: ${
                            response.errorBody()?.string()
                        }"
                    )
                    Result.failure(Exception("Places Nearby Search API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Places Nearby Search API network error", e)
                Result.failure(e)
            }
        }
    }

    // --- Favorite Route Functions ---
    private fun getFavoriteRoutesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("favoriteRoutes")

    suspend fun addFavoriteRoute(userId: String, route: FavoriteRoute): Result<String> {
        if (userId.isEmpty()) {
            Log.e("TrafficRepository", "User ID is empty, cannot add favorite route.")
            return Result.failure(IllegalArgumentException("User ID cannot be empty."))
        }
        Log.d("TrafficRepository", "Adding favorite route for user $userId: ${route.name}")
        return withContext(Dispatchers.IO) {
            try {
                // We assign the userId to the FavoriteRoute object before adding to Firestore.
                val routeWithUserId =
                    route.copy(userId = userId, createdAt = System.currentTimeMillis())
                val documentReference =
                    getFavoriteRoutesCollection(userId).add(routeWithUserId).await()
                Log.d(
                    "TrafficRepository",
                    "Favorite route added with ID: ${documentReference.id} for user $userId"
                )
                Result.success(documentReference.id)
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Error adding favorite route for user $userId", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getFavoriteRoutes(userId: String): Result<List<FavoriteRoute>> {
        if (userId.isEmpty()) {
            Log.w(
                "TrafficRepository",
                "User ID is empty, cannot fetch favorite routes. Returning empty list."
            )
            return Result.success(emptyList())
        }
        Log.d("TrafficRepository", "Fetching favorite routes for user $userId")
        return withContext(Dispatchers.IO) {
            try {
                val querySnapshot = getFavoriteRoutesCollection(userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val favoriteRoutes = querySnapshot.toObjects(FavoriteRoute::class.java)
                Log.d(
                    "TrafficRepository",
                    "Fetched ${favoriteRoutes.size} favorite routes for user $userId."
                )
                Result.success(favoriteRoutes)
            } catch (e: Exception) {
                Log.e("TrafficRepository", "Error fetching favorite routes for user $userId", e)
                Result.failure(e)
            }
        }
    }

    suspend fun deleteFavoriteRoute(userId: String, routeId: String): Result<Unit> {
        if (userId.isEmpty() || routeId.isEmpty()) {
            Log.e(
                "TrafficRepository",
                "User ID or Route ID is empty, cannot delete favorite route."
            )
            return Result.failure(IllegalArgumentException("User ID and Route ID cannot be empty."))
        }
        Log.d("TrafficRepository", "Deleting favorite route with ID: $routeId for user $userId")
        return withContext(Dispatchers.IO) {
            try {
                getFavoriteRoutesCollection(userId).document(routeId).delete().await()
                Log.d(
                    "TrafficRepository",
                    "Favorite route deleted successfully: $routeId for user $userId"
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(
                    "TrafficRepository",
                    "Error deleting favorite route $routeId for user $userId",
                    e
                )
                Result.failure(e)
            }
        }
    }
    // --- End of Favorite Route Functions ---
}
