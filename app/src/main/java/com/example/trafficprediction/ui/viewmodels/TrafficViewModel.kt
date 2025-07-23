package com.example.trafficprediction.ui.viewmodels

// @SuppressLint import removed as per user request.
import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trafficprediction.data.AuthRepository
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.data.TrafficRepository
import com.example.trafficprediction.network.Location
import com.example.trafficprediction.utils.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

val TimeOptions = listOf("8 AM", "2 PM", "8 PM")
val DayTypeOptions = listOf("Weekday", "Weekend")

class TrafficViewModel(application: Application) : AndroidViewModel(application) {

    // We create the PlacesClient within the ViewModel.
    private val placesClient = Places.createClient(application.applicationContext)
    private val repository = TrafficRepository(
        application.applicationContext,
        placesClient = placesClient
    ) // We pass placesClient to the repository.
    private val authRepository = AuthRepository()

    // --- StateFlows ---
    private val _startAddress = MutableStateFlow("")
    val startAddress: StateFlow<String> = _startAddress.asStateFlow()
    private val _endAddress = MutableStateFlow("")
    val endAddress: StateFlow<String> = _endAddress.asStateFlow()
    private val _selectedTime = MutableStateFlow(TimeOptions[0])
    val selectedTime: StateFlow<String> = _selectedTime.asStateFlow()
    private val _selectedDayType = MutableStateFlow(DayTypeOptions[0])
    val selectedDayType: StateFlow<String> = _selectedDayType.asStateFlow()
    private val _predictionResult = MutableStateFlow<String?>(null)
    val predictionResult: StateFlow<String?> = _predictionResult.asStateFlow()

    private val _isGeocoding = MutableStateFlow(false) // For the geocoding process.
    val isGeocoding: StateFlow<Boolean> = _isGeocoding.asStateFlow()
    private val _isFetchingPrediction = MutableStateFlow(false) // For the prediction API call.
    val isFetchingPrediction: StateFlow<Boolean> = _isFetchingPrediction.asStateFlow()

    // StateFlow for the route polyline.
    private val _routePolyline = MutableStateFlow<String?>(null)
    val routePolyline: StateFlow<String?> = _routePolyline.asStateFlow()

    // General isLoading, true if any loading operation is in progress.
    val isLoading: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        _isGeocoding,
        _isFetchingPrediction
    ) { geocoding, fetching ->
        geocoding || fetching
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)


    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _startCoordinates = MutableStateFlow<Location?>(null) // For the map.
    val startCoordinates: StateFlow<Location?> = _startCoordinates.asStateFlow()
    private val _endCoordinates = MutableStateFlow<Location?>(null) // For the map.
    val endCoordinates: StateFlow<Location?> = _endCoordinates.asStateFlow()
    private val _predictionHistory = MutableStateFlow<List<TrafficPredictionLog>>(emptyList())
    val predictionHistory: StateFlow<List<TrafficPredictionLog>> = _predictionHistory.asStateFlow()
    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()
    private val _historyErrorMessage = MutableStateFlow<String?>(null)
    val historyErrorMessage: StateFlow<String?> = _historyErrorMessage.asStateFlow()

    private val _selectedLog = MutableStateFlow<TrafficPredictionLog?>(null)
    val selectedLog: StateFlow<TrafficPredictionLog?> = _selectedLog.asStateFlow()

    // _currentUserLocation and currentUserLocation removed as per user request.

    private val _istanbulTrafficSummary =
        MutableStateFlow<List<TrafficRepository.RouteTrafficInfo>>(emptyList())
    val istanbulTrafficSummary: StateFlow<List<TrafficRepository.RouteTrafficInfo>> =
        _istanbulTrafficSummary.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    // --- Weather StateFlows ---
    private val _weatherCondition = MutableStateFlow<String?>(null) // e.g., "Rainy", "Sunny"
    val weatherCondition: StateFlow<String?> = _weatherCondition.asStateFlow()

    private val _weatherTemperature = MutableStateFlow<Double?>(null) // e.g., 25.0 (Celsius)
    val weatherTemperature: StateFlow<Double?> = _weatherTemperature.asStateFlow()

    private val _isFetchingWeather = MutableStateFlow(false)
    val isFetchingWeather: StateFlow<Boolean> = _isFetchingWeather.asStateFlow()

    private val _detailedWeatherInfo =
        MutableStateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?>(null)
    val detailedWeatherInfo: StateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?> =
        _detailedWeatherInfo.asStateFlow()

    // --- StateFlows for Current Device Location Weather ---
    private val _currentUserDeviceLocation = MutableStateFlow<LatLng?>(null)
    val currentUserDeviceLocation: StateFlow<LatLng?> = _currentUserDeviceLocation.asStateFlow()

    private val _currentLocationWeatherDetails = MutableStateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?>(null)
    val currentLocationWeatherDetails: StateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?> = _currentLocationWeatherDetails.asStateFlow()

    private val _isFetchingCurrentLocationWeather = MutableStateFlow(false)
    val isFetchingCurrentLocationWeather: StateFlow<Boolean> = _isFetchingCurrentLocationWeather.asStateFlow()
    // --- End of StateFlows for Current Device Location Weather ---

    // --- POI StateFlows ---
    private val _nearbyPlaces =
        MutableStateFlow<List<com.example.trafficprediction.network.PlaceResult>>(emptyList()) // Type updated.
    val nearbyPlaces: StateFlow<List<com.example.trafficprediction.network.PlaceResult>> =
        _nearbyPlaces.asStateFlow()

    private val _isFetchingPlaces = MutableStateFlow(false)
    val isFetchingPlaces: StateFlow<Boolean> = _isFetchingPlaces.asStateFlow()
    // --- End of POI StateFlows ---


    // --- Input Functions ---
    fun onStartAddressChange(value: String) {
        _startAddress.value = value
    }

    fun onEndAddressChange(value: String) {
        _endAddress.value = value
    }

    fun onTimeChange(value: String) {
        _selectedTime.value = value
    }

    fun onDayTypeChange(value: String) {
        _selectedDayType.value = value
    }

    fun onMapClick(latLng: LatLng, isStartPoint: Boolean) {
        viewModelScope.launch {
            _isGeocoding.value = true // Reverse geocoding starts.
            _errorMessage.value = null
            Log.d(
                "TrafficViewModel",
                "Map clicked at $latLng, for ${if (isStartPoint) "start" else "end"} point."
            )
            val result = repository.getAddressFromCoordinates(latLng.latitude, latLng.longitude)
            result.fold(
                onSuccess = { address ->
                    if (isStartPoint) {
                        _startAddress.value = address
                        _startCoordinates.value = Location(latLng.latitude, latLng.longitude)
                    } else {
                        _endAddress.value = address
                        _endCoordinates.value = Location(latLng.latitude, latLng.longitude)
                    }
                    Log.d("TrafficViewModel", "Address from map click: $address")
                },
                onFailure = { exception ->
                    _errorMessage.value = "Could not get address: ${exception.message}"
                    Log.e("TrafficViewModel", "Error getting address from coordinates", exception)
                }
            )
            _isGeocoding.value = false // Reverse geocoding finished.
        }
    }
    // --- End of Input Functions ---

    // --- Main Prediction Function ---
    fun fetchTrafficPredictionFromAddresses() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection available."
            return
        }

        val startAddr = _startAddress.value.trim()
        val endAddr = _endAddress.value.trim()

        _errorMessage.value = null
        _predictionResult.value = null
        _routePolyline.value = null // We clear the polyline before a new prediction.

        if (startAddr.isBlank() || endAddr.isBlank()) {
            _errorMessage.value = "Please enter both start and end locations."
            return
        }

        val currentTime = _selectedTime.value
        val timeInt: Int? = when (currentTime) {
            "8 AM" -> 8
            "2 PM" -> 14
            "8 PM" -> 20
            else -> null
        }

        val currentDayType = _selectedDayType.value
        val isWeekdayInt: Int? = when (currentDayType) {
            "Weekday" -> 1
            "Weekend" -> 0
            else -> null
        }

        if (timeInt == null || isWeekdayInt == null) {
            _errorMessage.value = "Invalid time or day type selected."
            return
        }

        viewModelScope.launch {
            _isGeocoding.value = true // Geocoding step starts.
            _isFetchingPrediction.value = false
            Log.d("TrafficViewModel", "Starting prediction for: '$startAddr' to '$endAddr'")

            var startLocResult: Result<Location>? = null
            var endLocResult: Result<Location>? = null

            try {
                var currentStartLoc = _startCoordinates.value
                var currentEndLoc = _endCoordinates.value

                if (currentStartLoc == null || _startAddress.value != startAddr) {
                    Log.d("TrafficViewModel", "Geocoding start address: $startAddr")
                    startLocResult = repository.getCoordinatesFromAddress(startAddr)
                    currentStartLoc = startLocResult?.getOrNull()
                    startLocResult?.onFailure {
                        _isGeocoding.value = false
                        throw it
                    }
                }
                if (currentEndLoc == null || _endAddress.value != endAddr) {
                    Log.d("TrafficViewModel", "Geocoding end address: $endAddr")
                    endLocResult = repository.getCoordinatesFromAddress(endAddr)
                    currentEndLoc = endLocResult?.getOrNull()
                    endLocResult?.onFailure {
                        _isGeocoding.value = false
                        throw it
                    }
                }
                _isGeocoding.value = false // Geocoding finished.

                // currentStartLoc is already defined and assigned above.
                // We only update currentEndLoc if endLocResult is not null.
                if (endLocResult != null) { // endLocResult can be null if geocoding wasn't performed.
                    currentEndLoc = endLocResult?.getOrNull()
                }


                startLocResult?.onFailure { throw it } // This is only relevant if start geocoding was done.
                endLocResult?.onFailure { throw it }   // This is only relevant if end geocoding was done.

                if (currentStartLoc != null && currentEndLoc != null) {
                    _startCoordinates.value = currentStartLoc
                    _endCoordinates.value = currentEndLoc

                    val startLat = currentStartLoc.latitude
                    val startLng = currentStartLoc.longitude
                    val endLat = currentEndLoc.latitude
                    val endLng = currentEndLoc.longitude

                    if (startLat == null || startLng == null || endLat == null || endLng == null) {
                        throw IllegalStateException("Geocoding returned null latitude or longitude.")
                    }

                    // We fetch the Route Polyline and Traffic Prediction concurrently.
                    viewModelScope.launch { // Fetch polyline in a separate coroutine.
                        Log.d("TrafficViewModel", "Fetching route polyline for map display...")
                        val polylineResult =
                            repository.getRoutePolyline(startLat, startLng, endLat, endLng)
                        polylineResult.fold(
                            onSuccess = { polyline ->
                                _routePolyline.value = polyline
                                Log.d("TrafficViewModel", "Route polyline fetched successfully.")
                            },
                            onFailure = { e ->
                                Log.e("TrafficViewModel", "Failed to fetch route polyline", e)
                                // Prediction can continue even if polyline fails; it just won't be drawn on the map.
                                // _errorMessage.value = "Could not fetch route for map: ${e.message}" // Optional error message.
                            }
                        )
                    }

                    Log.d(
                        "TrafficViewModel",
                        "Coordinates obtained (Start: $startLat, $startLng ; End: $endLat, $endLng). Fetching traffic prediction..."
                    )
                    _isFetchingPrediction.value = true // Prediction API call starts.
                    val trafficResult = repository.getTrafficPrediction(
                        startLng,
                        startLat,
                        endLng,
                        endLat,
                        timeInt,
                        isWeekdayInt
                    )
                    _isFetchingPrediction.value = false // Prediction API call finished.

                    trafficResult.fold(
                        onSuccess = { predictionResponse ->
                            val speed = predictionResponse.predictedSpeedKmh
                                ?: 0.0 // Updated to predictedSpeedKmh.
                            val condition = predictionResponse.estimatedCondition ?: "Unknown"
                            val distance = predictionResponse.segmentDistanceKm ?: 0.0 // New field.
                            val time =
                                predictionResponse.estimatedTravelTimeMinutes // New field, can be nullable.

                            val formattedSpeed = String.format("%.1f", speed)
                            val formattedDistance = String.format("%.1f", distance) // New field.
                            // If duration is null, show "N/A", otherwise format it.
                            val formattedTime =
                                time?.let { String.format("%.0f min", it) } ?: "N/A" // New field.

                            _predictionResult.value =
                                "Predicted Speed: $formattedSpeed km/h\n" + // Unit corrected to km/h.
                                        "Condition: $condition\n" +
                                        "Distance: $formattedDistance km\n" + // NEWLY ADDED
                                        "Est. Time: $formattedTime"       // NEWLY ADDED
                            Log.d(
                                "TrafficViewModel",
                                "API Success: Speed=$speed, Condition=$condition, Distance=$distance, Time=$time"
                            )

                            val logEntry = TrafficPredictionLog(
                                userId = authRepository.getCurrentUser()?.uid,
                                startAddress = startAddr,
                                endAddress = endAddr,
                                requestedTime = _selectedTime.value,
                                requestedDayType = _selectedDayType.value,
                                startLat = currentStartLoc.latitude, // currentStartLoc should not be null at this point.
                                startLng = currentStartLoc.longitude,
                                endLat = currentEndLoc.latitude,   // currentEndLoc should not be null at this point.
                                endLng = currentEndLoc.longitude,
                                predictedSpeed = predictionResponse.predictedSpeedKmh, // Updated to predictedSpeedKmh.
                                estimatedCondition = predictionResponse.estimatedCondition,
                                segmentDistanceKm = predictionResponse.segmentDistanceKm, // New field.
                                estimatedTravelTimeMinutes = predictionResponse.estimatedTravelTimeMinutes // New field.
                                // Timestamp will be added automatically by Firestore.
                            )
                            viewModelScope.launch {
                                repository.savePredictionLog(logEntry).onFailure { saveError ->
                                    Log.e(
                                        "TrafficViewModel",
                                        "Failed to save prediction log",
                                        saveError
                                    )
                                    _errorMessage.value = "Could not save prediction to history."
                                }
                            }
                        },
                        onFailure = { exception ->
                            _errorMessage.value =
                                "Traffic Prediction Error: ${exception.message ?: "Unknown error"}"
                            Log.e("TrafficViewModel", "Traffic API Failure", exception)
                        }
                    )
                } else {
                    throw IllegalStateException("Geocoding failed for one or both addresses.")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unknown error occurred"
                Log.e("TrafficViewModel", "Overall prediction flow error", e)
                _predictionResult.value = null
                // We don't reset coordinates on error, so the user can retry.
                // _startCoordinates.value = null
                // _endCoordinates.value = null
            } finally {
                _isGeocoding.value = false
                _isFetchingPrediction.value = false
                Log.d("TrafficViewModel", "Prediction flow finished.")
            }
        }
    }

    fun loadPredictionHistory() {
        viewModelScope.launch {
            _historyLoading.value = true
            _historyErrorMessage.value = null
            Log.d("TrafficViewModel", "Loading prediction history...")
            val result = repository.getPredictionHistory()
            result.fold(
                onSuccess = { historyList ->
                    _predictionHistory.value = historyList
                    Log.d("TrafficViewModel", "History loaded: ${historyList.size} items")
                },
                onFailure = { exception ->
                    _historyErrorMessage.value = "Error loading history: ${exception.message}"
                    Log.e("TrafficViewModel", "History loading failed", exception)
                }
            )
            _historyLoading.value = false
        }
    }

    fun selectLogForDetail(log: TrafficPredictionLog) {
        _selectedLog.value = log
        Log.d("TrafficViewModel", "Log selected for detail: ${log.logId}")
    }

    fun clearSelectedLog() {
        _selectedLog.value = null
        Log.d("TrafficViewModel", "Selected log cleared.")
    }

    fun loadIstanbulTrafficSummary() {
        viewModelScope.launch {
            if (_isSummaryLoading.value) return@launch // If already loading, don't start again.
            _isSummaryLoading.value = true
            Log.d("TrafficViewModel", "Loading Istanbul traffic summary...")

            // Main routes identified for Istanbul (Origin and Destination Addresses).
            // These addresses will be converted to coordinates via Geocoding API, or coordinates can be used directly.
            // For now, let's use address strings. For more precise results, place_id or lat,lng could be used.
            val routesToQuery = listOf(
                Triple(
                    "15 Temmuz Şehitler Köprüsü",
                    "Ortaköy, İstanbul",
                    "Beylerbeyi, İstanbul"
                ), // Approximate entry/exit points.
                Triple(
                    "FSM Köprüsü",
                    "Hisarüstü, İstanbul",
                    "Kavacık, İstanbul"
                ), // Approximate entry/exit points.
                Triple("E-5 Mecidiyeköy-Avcılar", "Mecidiyeköy, İstanbul", "Avcılar, İstanbul"),
                Triple(
                    "TEM Mahmutbey-Kurtköy",
                    "Mahmutbey Gişeler, İstanbul",
                    "Kurtköy Gişeler, İstanbul"
                )
            )

            val summaryResults = mutableListOf<TrafficRepository.RouteTrafficInfo>()
            var hasError = false

            coroutineScope { // We make all API calls in parallel.
                val deferredResults = routesToQuery.map { (routeName, origin, destination) ->
                    async<Result<TrafficRepository.RouteTrafficInfo>> { // Explicit return type added.
                        repository.getRouteTrafficInfo(origin, destination, routeName)
                    }
                }
                deferredResults.forEach { deferred ->
                    val result: Result<TrafficRepository.RouteTrafficInfo> =
                        deferred.await() // await() call and type assignment.
                    result.fold(
                        onSuccess = { info -> summaryResults.add(info) },
                        onFailure = { exception ->
                            Log.e(
                                "TrafficViewModel",
                                "Error fetching traffic for route: ${exception.message}"
                            )
                            hasError = true
                            // A single route error shouldn't prevent the whole summary; maybe we just don't show that route.
                            // For now, let's flag it for a general error message.
                        }
                    )
                }
            }

            if (hasError && summaryResults.isEmpty()) {
                _errorMessage.value = "Could not load complete traffic summary for Istanbul."
            } else {
                _errorMessage.value = null // If there are successful ones, clear the error.
            }
            _istanbulTrafficSummary.value = summaryResults
            _isSummaryLoading.value = false
            Log.d(
                "TrafficViewModel",
                "Istanbul traffic summary loaded: ${summaryResults.size} routes."
            )
        }
    }

    fun deletePredictionLog(logToDelete: TrafficPredictionLog) {
        viewModelScope.launch {
            // _historyLoading.value = true // Optional: loading indicator during deletion.
            // _historyErrorMessage.value = null
            if (logToDelete.logId == null) {
                Log.e("TrafficViewModel", "Cannot delete log, logId is null.")
                _historyErrorMessage.value = "Error: Cannot delete log without an ID."
                // _historyLoading.value = false
                return@launch
            }

            Log.d("TrafficViewModel", "Attempting to delete log: ${logToDelete.logId}")
            val result = repository.deletePredictionLog(logToDelete.logId)
            result.fold(
                onSuccess = {
                    Log.d(
                        "TrafficViewModel",
                        "Log ${logToDelete.logId} deleted successfully from Firestore."
                    )
                    // Update the local list.
                    _predictionHistory.value =
                        _predictionHistory.value.filterNot { it.logId == logToDelete.logId }
                    // Or reload the list: loadPredictionHistory()
                    // Reloading would sync states like deletion from another device but might be more costly.
                    // For now, let's remove it from the local list.
                },
                onFailure = { exception ->
                    Log.e(
                        "TrafficViewModel",
                        "Failed to delete log ${logToDelete.logId}",
                        exception
                    )
                    _historyErrorMessage.value = "Failed to delete prediction: ${exception.message}"
                }
            )
            // _historyLoading.value = false
        }
    }

    // --- Weather Functions ---

    // Call this function from UI after ensuring location permission is granted
    fun updateCurrentUserDeviceLocation(onLocationUpdated: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("TrafficViewModel", "updateCurrentUserDeviceLocation: Location permission not granted.")
            _errorMessage.value = "Location permission needed to get current weather."
            onLocationUpdated(null)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>().applicationContext)
        viewModelScope.launch {
            try {
                // Try to get current location with high accuracy
                val locationResult = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await() // Make sure to have kotlinx-coroutines-play-services for await()

                if (locationResult != null) {
                    val latLng = LatLng(locationResult.latitude, locationResult.longitude)
                    _currentUserDeviceLocation.value = latLng
                    Log.i("TrafficViewModel", "Current device location updated: $latLng")
                    onLocationUpdated(latLng)
                } else {
                    // If high accuracy fails, try last known location as a fallback
                    Log.w("TrafficViewModel", "Current location null, trying last known location.")
                    val lastLocation = fusedLocationClient.lastLocation.await()
                    if (lastLocation != null) {
                        val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                        _currentUserDeviceLocation.value = latLng
                        Log.i("TrafficViewModel", "Using last known device location: $latLng")
                        onLocationUpdated(latLng)
                    } else {
                        Log.e("TrafficViewModel", "Failed to get current device location (both current and last known are null).")
                        _errorMessage.value = "Could not retrieve current location."
                        _currentUserDeviceLocation.value = null
                        onLocationUpdated(null)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("TrafficViewModel", "SecurityException while getting current device location", e)
                _errorMessage.value = "Location permission error. Please check app settings."
                _currentUserDeviceLocation.value = null
                onLocationUpdated(null)
            } catch (e: Exception) {
                Log.e("TrafficViewModel", "Error getting current device location", e)
                _errorMessage.value = "Error getting current location."
                _currentUserDeviceLocation.value = null
                onLocationUpdated(null)
            }
        }
    }


    fun fetchWeatherForCurrentUserDeviceLocation() {
        val deviceLocation = _currentUserDeviceLocation.value
        if (deviceLocation == null) {
            _errorMessage.value = "Current device location is not available to fetch weather."
            Log.w("TrafficViewModel", "fetchWeatherForCurrentUserDeviceLocation: deviceLocation is null.")
            _currentLocationWeatherDetails.value = null
            return
        }

        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection for weather."
            _currentLocationWeatherDetails.value = null
            return
        }

        viewModelScope.launch {
            _isFetchingCurrentLocationWeather.value = true
            _currentLocationWeatherDetails.value = null // Clear previous
            Log.d("TrafficViewModel", "Fetching weather for current device location: Lat: ${deviceLocation.latitude}, Lon: ${deviceLocation.longitude}")
            val result = repository.getCurrentWeather(deviceLocation.latitude, deviceLocation.longitude)
            result.fold(
                onSuccess = { weatherResponse ->
                    _currentLocationWeatherDetails.value = weatherResponse
                    Log.i("TrafficViewModel", "Weather for current device location fetched: ${weatherResponse.weather?.firstOrNull()?.main}")
                },
                onFailure = { exception ->
                    Log.e("TrafficViewModel", "Failed to fetch weather for current device location", exception)
                    _errorMessage.value = "Could not fetch weather for your location: ${exception.message}"
                    _currentLocationWeatherDetails.value = null
                }
            )
            _isFetchingCurrentLocationWeather.value = false
        }
    }


    fun fetchCurrentWeatherForHomeScreen() { // This one uses start/end or default
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            // We could add a separate error message state for weather or use the general _errorMessage.
            // For now, let's log it and not show anything in the UI.
            Log.w("TrafficViewModel", "No internet for weather fetch.")
            _weatherCondition.value = "N/A (No Internet)" // Or it can be left null.
            _weatherTemperature.value = null
            return
        }

        // Location determination: First startCoordinates, then endCoordinates, then default (Istanbul).
        val locationToFetch = _startCoordinates.value
            ?: _endCoordinates.value
            ?: Location(latitude = 41.0082, longitude = 28.9784) // Istanbul default.

        locationToFetch.latitude?.let { lat ->
            locationToFetch.longitude?.let { lon ->
                viewModelScope.launch {
                    _isFetchingWeather.value = true
                    _weatherCondition.value = null // Clear previous value.
                    _weatherTemperature.value = null
                    Log.d("TrafficViewModel", "Fetching weather for Lat: $lat, Lon: $lon")
                    val result = repository.getCurrentWeather(lat, lon)
                    result.fold(
                        onSuccess = { weatherResponse ->
                            val mainCondition = weatherResponse.weather?.firstOrNull()?.main
                            val temp = weatherResponse.main?.temp
                            _weatherCondition.value = mainCondition ?: "Unknown"
                            _weatherTemperature.value = temp
                            _detailedWeatherInfo.value = weatherResponse // Also store detailed info.
                            Log.d(
                                "TrafficViewModel",
                                "Weather fetched: $mainCondition, Temp: $temp°C"
                            )
                        },
                        onFailure = { exception ->
                            Log.e("TrafficViewModel", "Failed to fetch weather", exception)
                            _weatherCondition.value = "N/A"
                            _weatherTemperature.value = null
                            _detailedWeatherInfo.value = null
                            // _errorMessage.value = "Could not fetch weather: ${exception.message}" // Optional.
                        }
                    )
                    _isFetchingWeather.value = false
                }
            } ?: run {
                Log.w("TrafficViewModel", "Latitude or Longitude is null for weather fetch.")
                _weatherCondition.value = "N/A (Location Invalid)"
                _weatherTemperature.value = null
                _detailedWeatherInfo.value = null
            }
        } ?: run {
            Log.w("TrafficViewModel", "Location for weather fetch is null.")
            _weatherCondition.value = "N/A (No Location)"
            _weatherTemperature.value = null
            _detailedWeatherInfo.value = null
        }
    }

    // --- POI Function ---
    fun fetchNearbyPlacesForCurrentLocation(placeType: String) {
        // This function requires getting the user's current location.
        // For now, if _startCoordinates is populated, let's use that; otherwise, log it.
        // In a real application, current location should be obtained via FusedLocationProviderClient and location permission checked.
        val currentLocation = _startCoordinates.value // Or a better location source.

        if (currentLocation?.latitude != null && currentLocation.longitude != null) {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _errorMessage.value = "No internet connection to fetch places."
                return
            }
            viewModelScope.launch {
                _isFetchingPlaces.value = true
                _nearbyPlaces.value = emptyList() // Clear previous results.
                Log.d(
                    "TrafficViewModel",
                    "Fetching nearby places of type '$placeType' for Lat: ${currentLocation.latitude}, Lon: ${currentLocation.longitude}"
                )
                val result = repository.findNearbyPlaces(
                    currentLocation.latitude!!,
                    currentLocation.longitude!!,
                    placeType
                )
                result.fold(
                    onSuccess = { places ->
                        _nearbyPlaces.value = places
                        Log.d("TrafficViewModel", "Fetched ${places.size} nearby places.")
                        if (places.isEmpty()) {
                            // _errorMessage.value = "No '$placeType' found nearby." // User can be informed.
                            Log.d("TrafficViewModel", "No '$placeType' found nearby.")
                        }
                    },
                    onFailure = { exception ->
                        Log.e("TrafficViewModel", "Failed to fetch nearby places", exception)
                        _errorMessage.value = "Could not fetch nearby places: ${exception.message}"
                    }
                )
                _isFetchingPlaces.value = false
            }
        } else {
            _errorMessage.value = "Current location not available to fetch nearby places."
            Log.w("TrafficViewModel", "Current location not available for fetching places.")
        }
    }
}
