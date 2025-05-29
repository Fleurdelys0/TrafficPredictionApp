package com.example.trafficprediction.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trafficprediction.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.trafficprediction.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Kimlik doğrulama durumunu temsil eden sealed class/interface (isteğe bağlı ama iyi pratik)
sealed interface AuthUiState {
    object Loading : AuthUiState
    object SignedOut : AuthUiState
    data class SignedIn(val user: FirebaseUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()

    // Kimlik doğrulama durumunu tutan StateFlow
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // --- UI State'leri (Giriş formu için) ---
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Giriş/kayıt işlemi için
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null) // Giriş/kayıt hataları için
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Görünen ad için StateFlow
    private val _newDisplayName = MutableStateFlow("")
    val newDisplayName: StateFlow<String> = _newDisplayName.asStateFlow()

    // Seçilen profil resmi URI'si için StateFlow
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // Mevcut kullanıcının profil resmi URL'si için StateFlow
    private val _currentUserPhotoUrl = MutableStateFlow<String?>(null)
    val currentUserPhotoUrl: StateFlow<String?> = _currentUserPhotoUrl.asStateFlow()

    // Kayıt sırasında girilen görünen ad için StateFlow
    private val _signUpDisplayName = MutableStateFlow("")
    val signUpDisplayName: StateFlow<String> = _signUpDisplayName.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>() // Başarı mesajlarını taşıyacak
    val eventFlow = _eventFlow.asSharedFlow()
    // --- ---

    init {
        // ViewModel başlatıldığında mevcut kullanıcı durumunu kontrol et
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            Log.d("AuthViewModel", "User already signed in: ${currentUser.email}, DisplayName: ${currentUser.displayName}, PhotoURL: ${currentUser.photoUrl}")
            _authUiState.value = AuthUiState.SignedIn(currentUser)
            _newDisplayName.value = currentUser.displayName ?: "" // Görünen adı yükle
            _currentUserPhotoUrl.value = currentUser.photoUrl?.toString() // Profil resmi URL'sini yükle
        } else {
            Log.d("AuthViewModel", "No user signed in.")
            _authUiState.value = AuthUiState.SignedOut
        }
    }

    // --- Input Değişiklikleri ---
    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun togglePasswordVisibility() { _isPasswordVisible.value = !_isPasswordVisible.value }
    fun onNewDisplayNameChange(value: String) { _newDisplayName.value = value } // Profildeki adı düzenlemek için
    fun onSignUpDisplayNameChange(value: String) { _signUpDisplayName.value = value } // Kayıttaki adı almak için
    fun onSelectedImageUriChange(uri: Uri?) { _selectedImageUri.value = uri }
    // --- ---

    // --- Kimlik Doğrulama ve Profil İşlemleri ---

    fun updateDisplayName() { // Sadece görünen adı güncellemek için ayrı bir fonksiyon
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection available."
            return
        }
        val currentDisplayName = _newDisplayName.value.trim()
        if (currentDisplayName.isEmpty()) {
            _errorMessage.value = "Display name cannot be empty."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            // Sadece displayName'i güncelle, photoUri null kalsın
            val result = authRepository.updateUserProfile(displayName = currentDisplayName, photoUri = null)
            result.fold(
                onSuccess = { updatedUser ->
                    _authUiState.value = AuthUiState.SignedIn(updatedUser)
                    _newDisplayName.value = updatedUser.displayName ?: ""
                    _currentUserPhotoUrl.value = updatedUser.photoUrl?.toString()
                    _eventFlow.emit("Display name updated successfully!")
                    Log.d("AuthViewModel", "Display name updated: ${updatedUser.displayName}")
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.localizedMessage ?: "Failed to update display name."
                    Log.e("AuthViewModel", "Display name update failed", exception)
                }
            )
            _isLoading.value = false
        }
    }

    fun uploadAndSetProfilePicture() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection available."
            return
        }
        val imageUriToUpload = _selectedImageUri.value
        if (imageUriToUpload == null) {
            _errorMessage.value = "Please select an image first."
            return
        }
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _errorMessage.value = "User not logged in."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d("AuthViewModel", "Starting profile picture upload for user: ${currentUser.uid}")

            // 1. Resmi Storage'a yükle
            val uploadResult = authRepository.uploadProfileImage(currentUser.uid, imageUriToUpload)
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    Log.d("AuthViewModel", "Image uploaded, download URL: $downloadUrl")
                    // 2. Kullanıcı profilini yeni resim URL'si ile güncelle
                    // displayName olarak _newDisplayName.value veya currentUser.displayName kullanılabilir.
                    // Eğer kullanıcı aynı anda hem adı hem resmi değiştirmek istiyorsa _newDisplayName.value daha mantıklı.
                    // Şimdilik sadece resmi güncellediğimizi varsayalım, adı değiştirmek için ayrı bir buton var.
                    val currentDisplayNameForUpdate = _newDisplayName.value.takeIf { it.isNotBlank() } ?: currentUser.displayName

                    val profileUpdateResult = authRepository.updateUserProfile(
                        displayName = currentDisplayNameForUpdate, // Mevcut veya yeni girilen adı kullan
                        photoUri = downloadUrl // Storage'dan alınan URL (Uri tipinde)
                    )
                    profileUpdateResult.fold(
                        onSuccess = { updatedUser ->
                            _authUiState.value = AuthUiState.SignedIn(updatedUser)
                            _newDisplayName.value = updatedUser.displayName ?: ""
                            _currentUserPhotoUrl.value = updatedUser.photoUrl?.toString()
                            _selectedImageUri.value = null // Seçimi temizle
                            _eventFlow.emit("Profile picture updated successfully!")
                            Log.d("AuthViewModel", "Profile picture updated for: ${updatedUser.email}")
                        },
                        onFailure = { e ->
                            _errorMessage.value = e.localizedMessage ?: "Failed to update profile with new picture."
                            Log.e("AuthViewModel", "Failed to update profile with picture URL", e)
                        }
                    )
                },
                onFailure = { e ->
                    _errorMessage.value = e.localizedMessage ?: "Failed to upload image."
                    Log.e("AuthViewModel", "Image upload failed", e)
                }
            )
            _isLoading.value = false
        }
    }


    fun signUp() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) { // AuthViewModel da AndroidViewModel olmalı
            _errorMessage.value = "No internet connection available."
            return
        }

        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()
        val currentSignUpDisplayName = _signUpDisplayName.value.trim() // Kayıt için girilen adı al

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _errorMessage.value = "Please enter email and password."
            return
        }
        if (currentSignUpDisplayName.isBlank()){ // Görünen ad boş olamaz
            _errorMessage.value = "Please enter a display name."
            return
        }
        // Şifre uzunluğu kontrolü gibi ek doğrulamalar eklenebilir

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d("AuthViewModel", "Sign up started for $currentEmail with display name $currentSignUpDisplayName")

            // AuthRepository'deki signUp fonksiyonuna displayName'i de gönder
            val result = authRepository.signUp(currentEmail, currentPassword, currentSignUpDisplayName)
            result.fold(
                onSuccess = { user ->
                    Log.d("AuthViewModel", "Sign up success: ${user.email}, DisplayName: ${user.displayName}")
                    _authUiState.value = AuthUiState.SignedIn(user)
                    _newDisplayName.value = user.displayName ?: "" // Profil için görünen adı ayarla
                    _currentUserPhotoUrl.value = user.photoUrl?.toString()
                    _eventFlow.emit("Registration successful!")
                    // Başarılı kayıt sonrası formu temizle
                    _email.value = ""
                    _password.value = ""
                    _signUpDisplayName.value = "" // Kayıt için girilen adı da temizle
                },
                onFailure = { exception ->
                    Log.e("AuthViewModel", "Sign up failed", exception)
                    // Hata mesajını daha kullanıcı dostu yapalım
                    _errorMessage.value = when (exception) {
                        is FirebaseAuthUserCollisionException -> "This email address is already in use."
                        is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use a stronger password."
                        // Diğer spesifik Firebase hataları buraya eklenebilir
                        else -> exception.localizedMessage ?: "Registration failed. Please try again." // Genel mesaj
                    }
                    _authUiState.value = AuthUiState.Error(errorMessage.value!!)
                }
            )
            _isLoading.value = false
        }
    }

    fun signIn() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) { // AuthViewModel da AndroidViewModel olmalı
            _errorMessage.value = "No internet connection available."
            return
        }

        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d("AuthViewModel", "Sign in started for $currentEmail")

            val result = authRepository.signIn(currentEmail, currentPassword)
            result.fold(
                onSuccess = { user ->
                    Log.d("AuthViewModel", "Sign in success: ${user.email}")
                    _authUiState.value = AuthUiState.SignedIn(user)
                    _newDisplayName.value = user.displayName ?: "" // Görünen adı yükle
                    _currentUserPhotoUrl.value = user.photoUrl?.toString() // Profil resmi URL'sini yükle
                    _eventFlow.emit("Login successful!")
                    // Başarılı giriş sonrası formu temizle (isteğe bağlı)
                    _email.value = ""
                    _password.value = ""
                },
                onFailure = { exception ->
                    Log.e("AuthViewModel", "Sign in failed", exception)
                    // Hata mesajını daha kullanıcı dostu yapalım
                    _errorMessage.value = when (exception) {
                        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        // Diğer spesifik Firebase hataları buraya eklenebilir
                        else -> exception.localizedMessage ?: "Login failed. Please check your credentials." // Genel mesaj
                    }
                    _authUiState.value = AuthUiState.Error(errorMessage.value!!)
                }
            )
            _isLoading.value = false
        }
    }

    fun signOut() {
        Log.d("AuthViewModel", "Sign out requested")
        authRepository.signOut()
        _authUiState.value = AuthUiState.SignedOut // UI'ı hemen güncelle
        // Formu temizle
        _email.value = ""
        _password.value = ""
        _errorMessage.value = null
        _newDisplayName.value = ""
        _currentUserPhotoUrl.value = null
        _selectedImageUri.value = null
        _signUpDisplayName.value = "" // Çıkışta kayıt için girilen adı da temizle
    }

    fun sendPasswordResetEmail() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "No internet connection available."
            return
        }
        val currentEmail = _email.value.trim()
        if (currentEmail.isBlank()) {
            _errorMessage.value = "Please enter your email address to reset password."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d("AuthViewModel", "Sending password reset email to $currentEmail")
            val result = authRepository.sendPasswordResetEmail(currentEmail)
            result.fold(
                onSuccess = {
                    _eventFlow.emit("Password reset email sent to $currentEmail. Please check your inbox.")
                    Log.d("AuthViewModel", "Password reset email sent successfully to $currentEmail")
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.localizedMessage ?: "Failed to send password reset email."
                    Log.e("AuthViewModel", "Failed to send password reset email", exception)
                }
            )
            _isLoading.value = false
        }
    }
}
