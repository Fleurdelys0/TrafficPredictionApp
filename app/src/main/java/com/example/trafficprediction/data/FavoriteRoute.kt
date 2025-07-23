package com.example.trafficprediction.data

import com.google.firebase.firestore.DocumentId

data class FavoriteRoute(
    @DocumentId // This will automatically assign the Firestore document ID to this field.
    val id: String = "",
    val userId: String = "", // We use this to specify which user this route belongs to.
    val name: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val originAddress: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationAddress: String = "",
    // Fields that we might add in the future:
    // val notificationEnabled: Boolean = true,
    // val notificationTime: String = "08:00" // e.g., in "HH:mm" format
    val createdAt: Long = System.currentTimeMillis() // Timestamp of when this route was created.
) {
    // A no-argument constructor is needed for Firestore, but it's already provided by Kotlin's default arguments.
    // constructor() : this("", "", "", 0.0, 0.0, "", 0.0, 0.0, "", 0L)
}
