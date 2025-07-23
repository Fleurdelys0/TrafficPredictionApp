package com.example.trafficprediction.data

import android.net.Uri
import android.util.Log
import com.example.trafficprediction.ui.viewmodels.UserProfileData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// This class will manage our authentication operations.
class AuthRepository {

    private val firebaseAuth: FirebaseAuth = Firebase.auth
    private val firebaseStorage = Firebase.storage
    private val firestore = Firebase.firestore // Our Firestore instance.

    // This returns the currently logged-in user (or null if none).
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    // Signing up with email, password, and display name.
    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        Log.d("AuthRepository", "Attempting sign up for: $email with displayName: $displayName")
        return withContext(Dispatchers.IO) {
            try {
                val authResult =
                    firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    // After the user is created, we update their displayName.
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                    }
                    user.updateProfile(profileUpdates).await()
                    Log.d(
                        "AuthRepository",
                        "Sign up successful for: ${user.email}, displayName set to: $displayName"
                    )
                    // We return the updated user info (to include the displayName).
                    Result.success(firebaseAuth.currentUser!!) // Get the most up-to-date user info.
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

    // Signing in with email and password.
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

    // Signing out the current user.
    fun signOut() {
        try {
            Log.d("AuthRepository", "Signing out user: ${firebaseAuth.currentUser?.email}")
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out", e)
        }
    }

    // Updating the user profile (display name and avatar icon name).
    suspend fun updateUserProfile(
        displayName: String?,
        avatarIconName: String?
    ): Result<FirebaseUser> {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w("AuthRepository", "No user logged in to update profile.")
            return Result.failure(Exception("User not logged in."))
        }

        Log.d(
            "AuthRepository",
            "Attempting to update profile for: ${user.email}. New displayName: $displayName, New avatarIconName: $avatarIconName"
        )
        return withContext(Dispatchers.IO) {
            try {
                // We update the Firebase Auth profile (only displayName here).
                val profileAuthUpdates = userProfileChangeRequest {
                    this.displayName = displayName
                    // photoUri cannot be directly stored as a string icon name in Firebase Auth,
                    // so we'll save it to Firestore.
                }
                user.updateProfile(profileAuthUpdates).await()
                Log.d(
                    "AuthRepository",
                    "Firebase Auth profile displayName updated for: ${user.email}."
                )

                // We update the user document in Firestore (for avatarIconName).
                if (avatarIconName != null) {
                    val userDocRef = firestore.collection("users").document(user.uid)
                    userDocRef.update("avatarIconName", avatarIconName).await()
                    Log.d(
                        "AuthRepository",
                        "Firestore avatarIconName updated for user: ${user.uid} to $avatarIconName"
                    )
                } else {
                    // If avatarIconName is null, we can remove this field from Firestore or leave it empty.
                    // For now, let's do nothing if it's null, or we could consider deleting the field.
                    // val userDocRef = firestore.collection("users").document(user.uid)
                    // userDocRef.update("avatarIconName", FieldValue.delete()).await()
                }

                val updatedUser = firebaseAuth.currentUser!! // Get the most up-to-date user info.
                Log.d(
                    "AuthRepository",
                    "Profile update process successful for: ${updatedUser.email}. New displayName: ${updatedUser.displayName}"
                )
                Result.success(updatedUser)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Profile update failed", e)
                Result.failure(e)
            }
        }
    }

    // Sending a password reset email.
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

    // Uploading a profile image to Firebase Storage and getting the download URL.
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

    // Reading additional user profile data (e.g., avatarIconName) from Firestore.
    suspend fun getUserProfileData(userId: String): Result<UserProfileData> {
        Log.d("AuthRepository", "Fetching user profile data for user: $userId")
        return withContext(Dispatchers.IO) {
            try {
                val documentSnapshot = firestore.collection("users").document(userId).get().await()
                if (documentSnapshot.exists()) {
                    val avatarIconName = documentSnapshot.getString("avatarIconName")
                    Log.d(
                        "AuthRepository",
                        "User profile data found for $userId. AvatarIconName: $avatarIconName"
                    )
                    Result.success(UserProfileData(avatarIconName = avatarIconName))
                } else {
                    Log.w(
                        "AuthRepository",
                        "No profile document found for user: $userId. Returning default."
                    )
                    Result.success(UserProfileData()) // If no document, we return default/empty data.
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to fetch user profile data for $userId", e)
                Result.failure(e)
            }
        }
    }
}
