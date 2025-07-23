package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// Main data class for the Places API Nearby Search response.
data class PlacesNearbySearchResponse(
    @SerializedName("html_attributions")
    val htmlAttributions: List<Any>?,
    @SerializedName("results")
    val results: List<PlaceResult>?,
    @SerializedName("status")
    val status: String?, // Possible values: OK, ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR
    @SerializedName("next_page_token")
    val nextPageToken: String?
)

// Data class containing details for each place.
data class PlaceResult(
    @SerializedName("business_status")
    val businessStatus: String?, // e.g., OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY
    @SerializedName("geometry")
    val geometry: PlaceGeometry?,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("icon_background_color")
    val iconBackgroundColor: String?,
    @SerializedName("icon_mask_base_uri")
    val iconMaskBaseUri: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("opening_hours")
    val openingHours: PlaceOpeningHours?,
    @SerializedName("photos")
    val photos: List<PlacePhoto>?,
    @SerializedName("place_id")
    val placeId: String?,
    @SerializedName("plus_code")
    val plusCode: PlacePlusCode?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("reference")
    val reference: String?,
    @SerializedName("scope")
    val scope: String?,
    @SerializedName("types")
    val types: List<String>?,
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int?,
    @SerializedName("vicinity")
    val vicinity: String? // A shorter form of the address.
)

// Place geometry (location).
data class PlaceGeometry(
    @SerializedName("location")
    val location: PlaceLocation?,
    @SerializedName("viewport")
    val viewport: PlaceViewport?
)

// Latitude and longitude.
data class PlaceLocation(
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lng")
    val lng: Double?
)

// Map viewport.
data class PlaceViewport(
    @SerializedName("northeast")
    val northeast: PlaceLocation?,
    @SerializedName("southwest")
    val southwest: PlaceLocation?
)

// Opening hours.
data class PlaceOpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean?
)

// Place photo.
data class PlacePhoto(
    @SerializedName("height")
    val height: Int?,
    @SerializedName("html_attributions")
    val htmlAttributions: List<String>?,
    @SerializedName("photo_reference")
    val photoReference: String?,
    @SerializedName("width")
    val width: Int?
)

// Plus Code (open location code).
data class PlacePlusCode(
    @SerializedName("compound_code")
    val compoundCode: String?,
    @SerializedName("global_code")
    val globalCode: String?
)
