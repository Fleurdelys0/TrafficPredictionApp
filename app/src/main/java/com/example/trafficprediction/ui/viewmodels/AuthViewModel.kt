package com.example.trafficprediction.ui.viewmodels

// We'll use specific ApiInstances instead of RetrofitInstance imports.
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.trafficprediction.data.AuthRepository
import com.example.trafficprediction.data.FavoriteRoute
import com.example.trafficprediction.data.TrafficRepository
import com.example.trafficprediction.network.GeocodingApiInstance
import com.example.trafficprediction.network.TrafficApiInstance
import com.example.trafficprediction.network.WeatherApiInstance
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Kullanıcının Firestore'daki ek profil bilgilerini tutmak için data class
data class UserProfileData(
    val avatarIconName: String? = null
    // We can add other fields here in the future.
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository() // Application parameter removed.

    // To initialize TrafficRepository, we need PlacesClient.
    // PlacesClient should be initialized with the Application context.
    // This might have been done in MainApplication, or we can do it here.
    // For now, we assume it's initialized in MainApplication.
    private val placesClient = Places.createClient(application.applicationContext)
    private val trafficRepository = TrafficRepository(
        context = application.applicationContext,
        trafficApiService = TrafficApiInstance.api, // Corrected.
        geocodingApiService = GeocodingApiInstance.api, // Corrected.
        weatherApiService = WeatherApiInstance.api, // Corrected.
        placesClient = placesClient
    )

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // We define the AuthState sealed class.
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser?) : AuthState() // FirebaseUser can be nullable.
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()


    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            _authState.value = AuthState.Authenticated(user) // We update the AuthState.
            loadFavoriteRoutes()
            loadUserProfileData(user.uid)
        } else {
            _authState.value = AuthState.Idle // Or a state like AuthState.Unauthenticated.
            _favoriteRoutes.value = emptyList()
            _userProfileData.value = null
        }
    }

    private val _favoriteRoutes = MutableStateFlow<List<FavoriteRoute>>(emptyList())
    val favoriteRoutes: StateFlow<List<FavoriteRoute>> = _favoriteRoutes.asStateFlow()

    private val _isLoadingFavorites = MutableStateFlow(false)
    val isLoadingFavorites: StateFlow<Boolean> = _isLoadingFavorites.asStateFlow()

    private val _favoriteRouteError = MutableStateFlow<String?>(null)
    val favoriteRouteError: StateFlow<String?> = _favoriteRouteError.asStateFlow()

    private val _userProfileData = MutableStateFlow<UserProfileData?>(null)
    val userProfileData: StateFlow<UserProfileData?> = _userProfileData.asStateFlow()

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        // We'll remove the listener when the ViewModel is cleared.
    }

    private fun loadUserProfileData(userId: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Loading user profile data for user: $userId")
            val result = authRepository.getUserProfileData(userId)
            result.fold(
                onSuccess = { data ->
                    _userProfileData.value = data
                    Log.d("AuthViewModel", "Successfully loaded user profile data: $data")
                },
                onFailure = { exception ->
                    Log.e("AuthViewModel", "Error loading user profile data for $userId", exception)
                    _userProfileData.value =
                        UserProfileData() // Default or empty data in case of error.
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    // _currentUser and _authState will be updated by authStateListener.
                    Log.d("AuthViewModel", "Sign in successful: ${user.email}")
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                    Log.e("AuthViewModel", "Sign in error", exception)
                }
            )
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password, displayName)
            result.fold(
                onSuccess = { user ->
                    // _currentUser and _authState will be updated by authStateListener.
                    Log.d("AuthViewModel", "Sign up successful: ${user.email}")
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                    Log.e("AuthViewModel", "Sign up error", exception)
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        // _currentUser and _authState will be updated by authStateListener.
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _authState.value =
                        AuthState.Idle // Or a custom state e.g., PasswordResetEmailSent.
                    Log.d("AuthViewModel", "Password reset email sent to $email")
                },
                onFailure = { exception ->
                    _authState.value =
                        AuthState.Error(exception.message ?: "Failed to send password reset email")
                    Log.e("AuthViewModel", "Error sending password reset email", exception)
                }
            )
        }
    }


    // --- Favorite Route Functions ---

    fun loadFavoriteRoutes() {
        val userId = currentUser.value?.uid
        if (userId == null) {
            _favoriteRouteError.value = "User not logged in."
            _favoriteRoutes.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoadingFavorites.value = true
            _favoriteRouteError.value = null
            Log.d("AuthViewModel", "Loading favorite routes for user: $userId")
            val result = trafficRepository.getFavoriteRoutes(userId)
            result.fold(
                onSuccess = { routes ->
                    _favoriteRoutes.value = routes
                    Log.d("AuthViewModel", "Successfully loaded ${routes.size} favorite routes.")
                },
                onFailure = { exception ->
                    _favoriteRouteError.value =
                        "Failed to load favorite routes: ${exception.message}"
                    Log.e("AuthViewModel", "Error loading favorite routes", exception)
                }
            )
            _isLoadingFavorites.value = false
        }
    }

    fun addFavoriteRoute(route: FavoriteRoute) { // This function takes a FavoriteRoute object directly; coordinates should already be resolved.
        val userId = currentUser.value?.uid
        if (userId == null) {
            _favoriteRouteError.value = "User not logged in. Cannot add route."
            return
        }
        val routeToAdd = route.copy(userId = userId) // We assign the userId here.
        viewModelScope.launch {
            _favoriteRouteError.value = null
            Log.d("AuthViewModel", "Adding favorite route: ${routeToAdd.name} for user $userId")
            val result = trafficRepository.addFavoriteRoute(userId, routeToAdd)
            result.fold(
                onSuccess = { routeId ->
                    Log.d(
                        "AuthViewModel",
                        "Favorite route added successfully with id: $routeId. Reloading routes."
                    )
                    loadFavoriteRoutes() // Refresh the list.
                },
                onFailure = { exception ->
                    _favoriteRouteError.value = "Failed to add favorite route: ${exception.message}"
                    Log.e("AuthViewModel", "Error adding favorite route", exception)
                }
            )
        }
    }

    fun addFavoriteRouteFromAddresses(
        routeName: String,
        originAddress: String,
        destinationAddress: String
    ) {
        val userId = currentUser.value?.uid
        if (userId == null) {
            _favoriteRouteError.value = "User not logged in. Cannot add route."
            return
        }
        if (routeName.isBlank() || originAddress.isBlank() || destinationAddress.isBlank()) {
            _favoriteRouteError.value =
                "Route name, origin, and destination addresses cannot be empty."
            return
        }

        viewModelScope.launch {
            _isLoadingFavorites.value = true // Start the loading indicator.
            _favoriteRouteError.value = null
            Log.d("AuthViewModel", "Attempting to add favorite route from addresses: $routeName")

            try {
                val originResult = trafficRepository.getCoordinatesFromAddress(originAddress)
                val destinationResult =
                    trafficRepository.getCoordinatesFromAddress(destinationAddress)

                var originLocation: com.example.trafficprediction.network.Location? = null
                var destinationLocation: com.example.trafficprediction.network.Location? = null
                var errorOccurred = false

                originResult.fold(
                    onSuccess = { location -> originLocation = location },
                    onFailure = { exception ->
                        _favoriteRouteError.value =
                            "Could not find coordinates for origin: ${exception.message}"
                        Log.e(
                            "AuthViewModel",
                            "Error geocoding origin address: $originAddress",
                            exception
                        )
                        errorOccurred = true
                    }
                )

                if (errorOccurred) {
                    _isLoadingFavorites.value = false
                    return@launch
                }

                destinationResult.fold(
                    onSuccess = { location -> destinationLocation = location },
                    onFailure = { exception ->
                        _favoriteRouteError.value =
                            "Could not find coordinates for destination: ${exception.message}"
                        Log.e(
                            "AuthViewModel",
                            "Error geocoding destination address: $destinationAddress",
                            exception
                        )
                        errorOccurred = true
                    }
                )

                if (errorOccurred) {
                    _isLoadingFavorites.value = false
                    return@launch
                }

                if (originLocation != null && destinationLocation != null) {
                    val newRoute = originLocation!!.latitude?.let {
                        FavoriteRoute(
                            userId = userId, // userId should be assigned here.
                            name = routeName,
                            originLat = it,
                            originLng = originLocation!!.longitude!!,
                            originAddress = originAddress, // We store the address entered by the user.
                            destinationLat = destinationLocation!!.latitude!!,
                            destinationLng = destinationLocation!!.longitude!!,
                            destinationAddress = destinationAddress // We store the address entered by the user.
                        )
                    }
                    if (newRoute != null) {
                        addFavoriteRoute(newRoute)
                    } // Call the existing addFavoriteRoute function.
                } else {
                    if (_favoriteRouteError.value == null) {
                        _favoriteRouteError.value =
                            "Could not resolve one or both addresses to coordinates."
                    }
                }
            } catch (e: Exception) {
                _favoriteRouteError.value = "An unexpected error occurred: ${e.message}"
                Log.e("AuthViewModel", "Unexpected error in addFavoriteRouteFromAddresses", e)
            } finally {
                _isLoadingFavorites.value = false // Stop the loading indicator.
            }
        }
    }


    fun deleteFavoriteRoute(routeId: String) {
        val userId = currentUser.value?.uid
        if (userId == null) {
            _favoriteRouteError.value = "User not logged in. Cannot delete route."
            return
        }
        viewModelScope.launch {
            _favoriteRouteError.value = null
            Log.d("AuthViewModel", "Deleting favorite route: $routeId for user $userId")
            val result = trafficRepository.deleteFavoriteRoute(userId, routeId)
            result.fold(
                onSuccess = {
                    Log.d("AuthViewModel", "Favorite route deleted successfully. Reloading routes.")
                    loadFavoriteRoutes() // Refresh the list.
                },
                onFailure = { exception ->
                    _favoriteRouteError.value =
                        "Failed to delete favorite route: ${exception.message}"
                    Log.e("AuthViewModel", "Error deleting favorite route", exception)
                }
            )
        }
    }

    fun clearFavoriteRouteError() {
        _favoriteRouteError.value = null
    }

    fun updateProfile(newName: String, avatarIconName: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateUserProfile(newName, avatarIconName)
            result.fold(
                onSuccess = { updatedUser ->
                    _currentUser.value = updatedUser // FirebaseUser updated.
                    updatedUser.uid?.let { loadUserProfileData(it) }
                    _authState.value = AuthState.Authenticated(updatedUser) // Update AuthState.
                    Log.d("AuthViewModel", "Profile updated successfully for ${updatedUser.email}.")
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Profile update failed")
                    Log.e("AuthViewModel", "Profile update error", exception)
                }
            )
        }
    }
}
