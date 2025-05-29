package com.example.trafficprediction.data

import android.net.Uri // Uri sınıfını import et
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest // userProfileChangeRequest import
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage // Firebase Storage KTX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Kimlik doğrulama işlemlerini yönetecek sınıf
class AuthRepository {

    private val firebaseAuth: FirebaseAuth = Firebase.auth
    private val firebaseStorage = Firebase.storage // Storage instance

    // Mevcut giriş yapmış kullanıcıyı döndürür (veya null)
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    // E-posta ve şifre ile kayıt olma
    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        Log.d("AuthRepository", "Attempting sign up for: $email with displayName: $displayName")
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    // Kullanıcı oluşturulduktan sonra displayName'i güncelle
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                    }
                    user.updateProfile(profileUpdates).await()
                    Log.d("AuthRepository", "Sign up successful for: ${user.email}, displayName set to: $displayName")
                    // Güncellenmiş kullanıcı bilgisini döndür (displayName'i içermesi için)
                    Result.success(firebaseAuth.currentUser!!) // En güncel kullanıcı bilgisini al
                } else {
                    Log.e("AuthRepository", "Sign up failed, user is null after creation.")
                    Result.failure(Exception("User creation failed."))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sign up failed", e)
                Result.failure(e)
            }
        }
    }

    // E-posta ve şifre ile giriş yapma
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        Log.d("AuthRepository", "Attempting sign in for: $email")
        return withContext(Dispatchers.IO) {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                Log.d("AuthRepository", "Sign in successful for: ${authResult.user?.email}")
                Result.success(authResult.user!!)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Sign in failed", e)
                Result.failure(e)
            }
        }
    }

    // Çıkış yapma
    fun signOut() {
        try {
            Log.d("AuthRepository", "Signing out user: ${firebaseAuth.currentUser?.email}")
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out", e)
        }
    }

    // Kullanıcı profilini güncelleme (görünen ad ve/veya fotoğraf URL'si)
    suspend fun updateUserProfile(displayName: String?, photoUri: Uri?): Result<FirebaseUser> {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w("AuthRepository", "No user logged in to update profile.")
            return Result.failure(Exception("User not logged in."))
        }

        Log.d("AuthRepository", "Attempting to update profile for: ${user.email}. New displayName: $displayName, New photoUri: $photoUri")
        return withContext(Dispatchers.IO) {
            try {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName
                    if (photoUri != null) {
                        this.photoUri = photoUri
                    }
                }

                user.updateProfile(profileUpdates).await()
                val updatedUser = firebaseAuth.currentUser!!
                Log.d("AuthRepository", "Profile update successful for: ${updatedUser.email}. New displayName: ${updatedUser.displayName}, New photoUri: ${updatedUser.photoUrl}")
                Result.success(updatedUser)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Profile update failed", e)
                Result.failure(e)
            }
        }
    }

    // Şifre sıfırlama e-postası gönderme
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        Log.d("AuthRepository", "Attempting to send password reset email to: $email")
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Log.d("AuthRepository", "Password reset email sent successfully to: $email")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to send password reset email to: $email", e)
                Result.failure(e)
            }
        }
    }

    // Profil resmini Firebase Storage'a yükleme ve indirme URL'sini alma
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<Uri> {
        Log.d("AuthRepository", "Uploading profile image for user: $userId from Uri: $imageUri")
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "profile_images/$userId/profile.jpg"
                val storageRef = firebaseStorage.reference.child(fileName)

                storageRef.putFile(imageUri).await()
                Log.d("AuthRepository", "Image uploaded successfully to: $fileName")

                val downloadUrl = storageRef.downloadUrl.await()
                Log.d("AuthRepository", "Download URL obtained: $downloadUrl")
                Result.success(downloadUrl)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to upload profile image or get download URL", e)
                Result.failure(e)
            }
        }
    }
}
