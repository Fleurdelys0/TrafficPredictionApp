package com.example.trafficprediction.ui.viewmodels

// import android.annotation.SuppressLint // Kullanıcı isteği üzerine kaldırıldı
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trafficprediction.data.AuthRepository
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.data.TrafficRepository
import com.example.trafficprediction.network.Location
import com.example.trafficprediction.utils.NetworkUtils
// import com.google.android.gms.location.LocationServices // Kullanıcı isteği üzerine kaldırıldı
import com.google.android.gms.maps.model.LatLng // LatLng import
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place

val TimeOptions = listOf("8 AM", "2 PM", "8 PM")
val DayTypeOptions = listOf("Weekday", "Weekend")

class TrafficViewModel(application: Application) : AndroidViewModel(application) {

    // PlacesClient'ı ViewModel içinde oluştur
    private val placesClient = Places.createClient(application.applicationContext)
    private val repository = TrafficRepository(application.applicationContext, placesClient = placesClient) // placesClient'ı repository'ye ilet
    private val authRepository = AuthRepository()

    // --- StateFlow'lar ---
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

    private val _isGeocoding = MutableStateFlow(false) // Geocoding işlemi için
    val isGeocoding: StateFlow<Boolean> = _isGeocoding.asStateFlow()
    private val _isFetchingPrediction = MutableStateFlow(false) // Tahmin API çağrısı için
    val isFetchingPrediction: StateFlow<Boolean> = _isFetchingPrediction.asStateFlow()

    // Rota polyline'ı için StateFlow
    private val _routePolyline = MutableStateFlow<String?>(null)
    val routePolyline: StateFlow<String?> = _routePolyline.asStateFlow()

    // Genel isLoading, herhangi bir yükleme işlemi varsa true olur
    val isLoading: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(_isGeocoding, _isFetchingPrediction) { geocoding, fetching ->
        geocoding || fetching
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)


    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _startCoordinates = MutableStateFlow<Location?>(null) // Harita için
    val startCoordinates: StateFlow<Location?> = _startCoordinates.asStateFlow()
    private val _endCoordinates = MutableStateFlow<Location?>(null) // Harita için
    val endCoordinates: StateFlow<Location?> = _endCoordinates.asStateFlow()
    private val _predictionHistory = MutableStateFlow<List<TrafficPredictionLog>>(emptyList())
    val predictionHistory: StateFlow<List<TrafficPredictionLog>> = _predictionHistory.asStateFlow()
    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()
    private val _historyErrorMessage = MutableStateFlow<String?>(null)
    val historyErrorMessage: StateFlow<String?> = _historyErrorMessage.asStateFlow()

    private val _selectedLog = MutableStateFlow<TrafficPredictionLog?>(null)
    val selectedLog: StateFlow<TrafficPredictionLog?> = _selectedLog.asStateFlow()

    // private val _currentUserLocation = MutableStateFlow<LatLng?>(null) // Kullanıcı isteği üzerine kaldırıldı
    // val currentUserLocation: StateFlow<LatLng?> = _currentUserLocation.asStateFlow() // Kullanıcı isteği üzerine kaldırıldı

    private val _istanbulTrafficSummary = MutableStateFlow<List<TrafficRepository.RouteTrafficInfo>>(emptyList())
    val istanbulTrafficSummary: StateFlow<List<TrafficRepository.RouteTrafficInfo>> = _istanbulTrafficSummary.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    // --- Hava Durumu StateFlow'ları ---
    private val _weatherCondition = MutableStateFlow<String?>(null) // Örn: "Rainy", "Sunny"
    val weatherCondition: StateFlow<String?> = _weatherCondition.asStateFlow()

    private val _weatherTemperature = MutableStateFlow<Double?>(null) // Örn: 25.0 (Celsius)
    val weatherTemperature: StateFlow<Double?> = _weatherTemperature.asStateFlow()

    private val _isFetchingWeather = MutableStateFlow(false)
    val isFetchingWeather: StateFlow<Boolean> = _isFetchingWeather.asStateFlow()

    private val _detailedWeatherInfo = MutableStateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?>(null)
    val detailedWeatherInfo: StateFlow<com.example.trafficprediction.network.CurrentWeatherResponse?> = _detailedWeatherInfo.asStateFlow()

    // --- POI StateFlow'ları ---
    private val _nearbyPlaces = MutableStateFlow<List<com.example.trafficprediction.network.PlaceResult>>(emptyList()) // Tip güncellendi
    val nearbyPlaces: StateFlow<List<com.example.trafficprediction.network.PlaceResult>> = _nearbyPlaces.asStateFlow()

    private val _isFetchingPlaces = MutableStateFlow(false)
    val isFetchingPlaces: StateFlow<Boolean> = _isFetchingPlaces.asStateFlow()
    // --- ---


    // --- Input Fonksiyonları ---
    fun onStartAddressChange(value: String) { _startAddress.value = value }
    fun onEndAddressChange(value: String) { _endAddress.value = value }
    fun onTimeChange(value: String) { _selectedTime.value = value }
    fun onDayTypeChange(value: String) { _selectedDayType.value = value }

    fun onMapClick(latLng: LatLng, isStartPoint: Boolean) {
        viewModelScope.launch {
            _isGeocoding.value = true // Ters geocoding başlıyor
            _errorMessage.value = null
            Log.d("TrafficViewModel", "Map clicked at $latLng, for ${if (isStartPoint) "start" else "end"} point.")
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
            _isGeocoding.value = false // Ters geocoding bitti
        }
    }
    // --- ---

    // --- Ana Tahmin Fonksiyonu ---
    fun fetchTrafficPredictionFromAddresses() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection available."
            return
        }

        val startAddr = _startAddress.value.trim()
        val endAddr = _endAddress.value.trim()

        _errorMessage.value = null
        _predictionResult.value = null
        _routePolyline.value = null // Yeni tahmin öncesi polyline'ı temizle

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
            _isGeocoding.value = true // Geocoding adımı başlıyor
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
                _isGeocoding.value = false // Geocoding bitti

                // currentStartLoc zaten yukarıda tanımlı ve atanmış durumda.
                // Sadece endLocResult null değilse currentEndLoc'u güncelle.
                if (endLocResult != null) { // endLocResult null olabilir eğer geocoding yapılmadıysa
                    currentEndLoc = endLocResult?.getOrNull()
                }


                startLocResult?.onFailure { throw it } // Bu, sadece start geocoding yapıldıysa anlamlı
                endLocResult?.onFailure { throw it }   // Bu, sadece end geocoding yapıldıysa anlamlı

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

                    // Rota Polyline'ını ve Trafik Tahminini eş zamanlı al
                    viewModelScope.launch { // Ayrı bir coroutine içinde polyline çekme
                        Log.d("TrafficViewModel", "Fetching route polyline for map display...")
                        val polylineResult = repository.getRoutePolyline(startLat, startLng, endLat, endLng)
                        polylineResult.fold(
                            onSuccess = { polyline ->
                                _routePolyline.value = polyline
                                Log.d("TrafficViewModel", "Route polyline fetched successfully.")
                            },
                            onFailure = { e ->
                                Log.e("TrafficViewModel", "Failed to fetch route polyline", e)
                                // Polyline alınamasa bile tahmin devam edebilir, sadece haritada çizilmez.
                                // _errorMessage.value = "Could not fetch route for map: ${e.message}" // İsteğe bağlı hata mesajı
                            }
                        )
                    }

                    Log.d("TrafficViewModel", "Coordinates obtained (Start: $startLat, $startLng ; End: $endLat, $endLng). Fetching traffic prediction...")
                    _isFetchingPrediction.value = true // Tahmin API çağrısı başlıyor
                    val trafficResult = repository.getTrafficPrediction(startLng, startLat, endLng, endLat, timeInt, isWeekdayInt)
                    _isFetchingPrediction.value = false // Tahmin API çağrısı bitti

                    trafficResult.fold(
                        onSuccess = { predictionResponse ->
                            val speed = predictionResponse.predictedSpeedKmh ?: 0.0 // predictedSpeedKmh olarak güncellendi
                            val condition = predictionResponse.estimatedCondition ?: "Unknown"
                            val distance = predictionResponse.segmentDistanceKm ?: 0.0 // YENİ
                            val time = predictionResponse.estimatedTravelTimeMinutes // YENİ, Nullable kalabilir

                            val formattedSpeed = String.format("%.1f", speed)
                            val formattedDistance = String.format("%.1f", distance) // YENİ
                            // Süre null ise "N/A" göster, değilse formatla
                            val formattedTime = time?.let { String.format("%.0f min", it) } ?: "N/A" // YENİ

                            _predictionResult.value = "Predicted Speed: $formattedSpeed km/h\n" + // Birim km/h olarak düzeltildi
                                                      "Condition: $condition\n" +
                                                      "Distance: $formattedDistance km\n" + // YENİ EKLENDİ
                                                      "Est. Time: $formattedTime"       // YENİ EKLENDİ
                            Log.d("TrafficViewModel", "API Success: Speed=$speed, Condition=$condition, Distance=$distance, Time=$time")

                            val logEntry = TrafficPredictionLog(
                                userId = authRepository.getCurrentUser()?.uid,
                                startAddress = startAddr,
                                endAddress = endAddr,
                                requestedTime = _selectedTime.value,
                                requestedDayType = _selectedDayType.value,
                                startLat = currentStartLoc.latitude, // currentStartLoc null olmamalı bu noktada
                                startLng = currentStartLoc.longitude,
                                endLat = currentEndLoc.latitude,   // currentEndLoc null olmamalı bu noktada
                                endLng = currentEndLoc.longitude,
                                predictedSpeed = predictionResponse.predictedSpeedKmh, // predictedSpeedKmh olarak güncellendi
                                estimatedCondition = predictionResponse.estimatedCondition,
                                segmentDistanceKm = predictionResponse.segmentDistanceKm, // YENİ
                                estimatedTravelTimeMinutes = predictionResponse.estimatedTravelTimeMinutes // YENİ
                                // timestamp Firestore tarafından otomatik eklenecek
                            )
                            viewModelScope.launch {
                                repository.savePredictionLog(logEntry).onFailure { saveError ->
                                    Log.e("TrafficViewModel", "Failed to save prediction log", saveError)
                                    _errorMessage.value = "Could not save prediction to history."
                                }
                            }
                        },
                        onFailure = { exception ->
                            _errorMessage.value = "Traffic Prediction Error: ${exception.message ?: "Unknown error"}"
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
                // _startCoordinates.value = null // Hata durumunda koordinatları sıfırlama, kullanıcı tekrar deneyebilir
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
            if (_isSummaryLoading.value) return@launch // Zaten yükleniyorsa tekrar başlatma
            _isSummaryLoading.value = true
            Log.d("TrafficViewModel", "Loading Istanbul traffic summary...")

            // İstanbul için belirlenen ana rotalar (Başlangıç ve Bitiş Adresleri)
            // Bu adresler Geocoding API ile koordinata çevrilecek veya doğrudan koordinat kullanılabilir.
            // Şimdilik adres string'leri kullanalım. Daha hassas sonuçlar için place_id veya lat,lng kullanılabilir.
            val routesToQuery = listOf(
                Triple("15 Temmuz Şehitler Köprüsü", "Ortaköy, İstanbul", "Beylerbeyi, İstanbul"), // Yaklaşık giriş/çıkışlar
                Triple("FSM Köprüsü", "Hisarüstü, İstanbul", "Kavacık, İstanbul"), // Yaklaşık giriş/çıkışlar
                Triple("E-5 Mecidiyeköy-Avcılar", "Mecidiyeköy, İstanbul", "Avcılar, İstanbul"),
                Triple("TEM Mahmutbey-Kurtköy", "Mahmutbey Gişeler, İstanbul", "Kurtköy Gişeler, İstanbul")
            )

            val summaryResults = mutableListOf<TrafficRepository.RouteTrafficInfo>()
            var hasError = false

            coroutineScope { // Tüm API çağrılarını paralel yap
                val deferredResults = routesToQuery.map { (routeName, origin, destination) ->
                    async<Result<TrafficRepository.RouteTrafficInfo>> { // Açık dönüş tipi eklendi
                        repository.getRouteTrafficInfo(origin, destination, routeName)
                    }
                }
                deferredResults.forEach { deferred ->
                    val result: Result<TrafficRepository.RouteTrafficInfo> = deferred.await() // await() çağrısı ve tip ataması
                    result.fold(
                        onSuccess = { info -> summaryResults.add(info) },
                        onFailure = { exception ->
                            Log.e("TrafficViewModel", "Error fetching traffic for route: ${exception.message}")
                            hasError = true
                            // Tek bir rota hatası tüm özeti engellemesin, belki sadece o rotayı göstermeyiz.
                            // Şimdilik genel bir hata mesajı için işaretleyelim.
                        }
                    )
                }
            }

            if (hasError && summaryResults.isEmpty()) {
                _errorMessage.value = "Could not load complete traffic summary for Istanbul."
            } else {
                _errorMessage.value = null // Başarılı olanlar varsa hatayı temizle
            }
            _istanbulTrafficSummary.value = summaryResults
            _isSummaryLoading.value = false
            Log.d("TrafficViewModel", "Istanbul traffic summary loaded: ${summaryResults.size} routes.")
        }
    }

    /* // Kullanıcı isteği üzerine kaldırıldı
    @SuppressLint("MissingPermission")
    fun fetchCurrentUserLocation() {
        Log.d("TrafficViewModel", "Fetching current user location...")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>().applicationContext)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        _currentUserLocation.value = latLng
                        Log.d("TrafficViewModel", "Current location found: $latLng")
                    } else {
                        Log.w("TrafficViewModel", "Last known location is null.")
                        _errorMessage.value = "Could not retrieve current location. Please ensure location services are enabled and try again."
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TrafficViewModel", "Error getting current location", e)
                    _errorMessage.value = "Error getting current location: ${e.message}"
                }
        } catch (e: SecurityException) { // Catch SecurityException just in case
            Log.e("TrafficViewModel", "SecurityException while fetching current location", e)
            _errorMessage.value = "Location permission error. Please check app permissions."
        }
    }
    */

    fun deletePredictionLog(logToDelete: TrafficPredictionLog) {
        viewModelScope.launch {
            // _historyLoading.value = true // Opsiyonel: Silme sırasında yükleme göstergesi
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
                    Log.d("TrafficViewModel", "Log ${logToDelete.logId} deleted successfully from Firestore.")
                    // Yerel listeyi güncelle
                    _predictionHistory.value = _predictionHistory.value.filterNot { it.logId == logToDelete.logId }
                    // Veya listeyi yeniden yükle: loadPredictionHistory()
                    // Yeniden yüklemek, başka bir cihazdan silme gibi durumları da senkronize eder ama daha maliyetli olabilir.
                    // Şimdilik yerel listeden çıkaralım.
                },
                onFailure = { exception ->
                    Log.e("TrafficViewModel", "Failed to delete log ${logToDelete.logId}", exception)
                    _historyErrorMessage.value = "Failed to delete prediction: ${exception.message}"
                }
            )
            // _historyLoading.value = false
        }
    }

    // --- Hava Durumu Fonksiyonu ---
    fun fetchCurrentWeatherForHomeScreen() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            // Hava durumu için ayrı bir hata mesajı state'i eklenebilir veya genel _errorMessage kullanılabilir.
            // Şimdilik loglayalım ve UI'da bir şey göstermeyelim.
            Log.w("TrafficViewModel", "No internet for weather fetch.")
            _weatherCondition.value = "N/A (No Internet)" // Veya null bırakılabilir
            _weatherTemperature.value = null
            return
        }

        // Konum belirleme: Önce startCoordinates, sonra endCoordinates, sonra varsayılan (İstanbul)
        val locationToFetch = _startCoordinates.value
            ?: _endCoordinates.value
            ?: Location(latitude = 41.0082, longitude = 28.9784) // İstanbul varsayılan

        locationToFetch.latitude?.let { lat ->
            locationToFetch.longitude?.let { lon ->
                viewModelScope.launch {
                    _isFetchingWeather.value = true
                    _weatherCondition.value = null // Önceki değeri temizle
                    _weatherTemperature.value = null
                    Log.d("TrafficViewModel", "Fetching weather for Lat: $lat, Lon: $lon")
                    val result = repository.getCurrentWeather(lat, lon)
                    result.fold(
                        onSuccess = { weatherResponse ->
                            val mainCondition = weatherResponse.weather?.firstOrNull()?.main
                            val temp = weatherResponse.main?.temp
                            _weatherCondition.value = mainCondition ?: "Unknown"
                            _weatherTemperature.value = temp
                            _detailedWeatherInfo.value = weatherResponse // Detaylı bilgiyi de sakla
                            Log.d("TrafficViewModel", "Weather fetched: $mainCondition, Temp: $temp°C")
                        },
                        onFailure = { exception ->
                            Log.e("TrafficViewModel", "Failed to fetch weather", exception)
                            _weatherCondition.value = "N/A"
                            _weatherTemperature.value = null
                            _detailedWeatherInfo.value = null
                            // _errorMessage.value = "Could not fetch weather: ${exception.message}" // Opsiyonel
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

    // --- POI Fonksiyonu ---
    fun fetchNearbyPlacesForCurrentLocation(placeType: String) {
        // Bu fonksiyon kullanıcının o anki konumunu almayı gerektirir.
        // Şimdilik, eğer _startCoordinates doluysa onu kullanalım, değilse loglayalım.
        // Gerçek bir uygulamada, FusedLocationProviderClient ile anlık konum alınmalı ve konum izni kontrol edilmeli.
        val currentLocation = _startCoordinates.value // Veya daha iyi bir konum kaynağı

        if (currentLocation?.latitude != null && currentLocation.longitude != null) {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _errorMessage.value = "No internet connection to fetch places."
                return
            }
            viewModelScope.launch {
                _isFetchingPlaces.value = true
                _nearbyPlaces.value = emptyList() // Önceki sonuçları temizle
                Log.d("TrafficViewModel", "Fetching nearby places of type '$placeType' for Lat: ${currentLocation.latitude}, Lon: ${currentLocation.longitude}")
                val result = repository.findNearbyPlaces(currentLocation.latitude!!, currentLocation.longitude!!, placeType)
                result.fold(
                    onSuccess = { places ->
                        _nearbyPlaces.value = places
                        Log.d("TrafficViewModel", "Fetched ${places.size} nearby places.")
                        if (places.isEmpty()) {
                            // _errorMessage.value = "No '$placeType' found nearby." // Kullanıcıya bilgi verilebilir
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
