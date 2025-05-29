package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

// Places API Nearby Search yanıtı için ana data class
data class PlacesNearbySearchResponse(
    @SerializedName("html_attributions")
    val htmlAttributions: List<Any>?,
    @SerializedName("results")
    val results: List<PlaceResult>?,
    @SerializedName("status")
    val status: String?, // OK, ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR
    @SerializedName("next_page_token")
    val nextPageToken: String?
)

// Her bir yer (place) için detayları içeren data class
data class PlaceResult(
    @SerializedName("business_status")
    val businessStatus: String?, // OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY
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
    val vicinity: String? // Adresin daha kısa bir formu
)

// Yer geometrisi (konum)
data class PlaceGeometry(
    @SerializedName("location")
    val location: PlaceLocation?,
    @SerializedName("viewport")
    val viewport: PlaceViewport?
)

// Enlem ve boylam
data class PlaceLocation(
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lng")
    val lng: Double?
)

// Harita görünüm alanı
data class PlaceViewport(
    @SerializedName("northeast")
    val northeast: PlaceLocation?,
    @SerializedName("southwest")
    val southwest: PlaceLocation?
)

// Açılış saatleri
data class PlaceOpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean?
)

// Yer fotoğrafı
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

// Plus Code (açık konum kodu)
data class PlacePlusCode(
    @SerializedName("compound_code")
    val compoundCode: String?,
    @SerializedName("global_code")
    val globalCode: String?
)
