package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("geocoded_waypoints")
    val geocodedWaypoints: List<GeocodedWaypoint>?,
    @SerializedName("routes")
    val routes: List<Route>?,
    @SerializedName("status")
    val status: String? // Possible values: OK, NOT_FOUND, ZERO_RESULTS, MAX_WAYPOINTS_EXCEEDED, INVALID_REQUEST, OVER_QUERY_LIMIT, REQUEST_DENIED, UNKNOWN_ERROR
)

data class GeocodedWaypoint(
    @SerializedName("geocoder_status")
    val geocoderStatus: String?,
    @SerializedName("place_id")
    val placeId: String?,
    @SerializedName("types")
    val types: List<String>?
)

data class Route(
    @SerializedName("summary")
    val summary: String?,
    @SerializedName("legs")
    val legs: List<Leg>?,
    @SerializedName("copyrights")
    val copyrights: String?,
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline?,
    @SerializedName("warnings")
    val warnings: List<String>?,
    @SerializedName("waypoint_order")
    val waypointOrder: List<Int>?,
    @SerializedName("bounds")
    val bounds: Bounds?
)

data class Leg(
    @SerializedName("distance")
    val distance: Distance?,
    @SerializedName("duration")
    val duration: Duration?, // Normal duration without traffic.
    @SerializedName("duration_in_traffic")
    val durationInTraffic: Duration?, // Estimated duration considering traffic conditions.
    @SerializedName("end_address")
    val endAddress: String?,
    @SerializedName("end_location")
    val endLocation: LocationDetails?,
    @SerializedName("start_address")
    val startAddress: String?,
    @SerializedName("start_location")
    val startLocation: LocationDetails?,
    @SerializedName("steps")
    val steps: List<Step>?,
    @SerializedName("traffic_speed_entry")
    val trafficSpeedEntry: List<Any>?, // Detailed traffic information, using Any for now.
    @SerializedName("via_waypoint")
    val viaWaypoint: List<Any>?
)

data class Distance(
    @SerializedName("text")
    val text: String?, // e.g., "10.5 km"
    @SerializedName("value")
    val value: Int? // Value in meters.
)

data class Duration(
    @SerializedName("text")
    val text: String?, // e.g., "35 mins"
    @SerializedName("value")
    val value: Int? // Value in seconds.
)

data class LocationDetails( // Named differently to avoid conflict with GeocodingResponse.Location.
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lng")
    val lng: Double?
)

data class Step(
    @SerializedName("distance")
    val distance: Distance?,
    @SerializedName("duration")
    val duration: Duration?,
    @SerializedName("end_location")
    val endLocation: LocationDetails?,
    @SerializedName("html_instructions")
    val htmlInstructions: String?,
    @SerializedName("polyline")
    val polyline: OverviewPolyline?,
    @SerializedName("start_location")
    val startLocation: LocationDetails?,
    @SerializedName("travel_mode")
    val travelMode: String?, // e.g., DRIVING
    @SerializedName("maneuver")
    val maneuver: String?
)

data class OverviewPolyline(
    @SerializedName("points")
    val points: String?
)

data class Bounds(
    @SerializedName("northeast")
    val northeast: LocationDetails?,
    @SerializedName("southwest")
    val southwest: LocationDetails?
)
