package com.example.trafficprediction.network

import com.google.gson.annotations.SerializedName

data class CurrentWeatherResponse(
    @SerializedName("coord")
    val coord: Coord?,
    @SerializedName("weather")
    val weather: List<WeatherDescription>?,
    @SerializedName("base")
    val base: String?,
    @SerializedName("main")
    val main: WeatherMain?,
    @SerializedName("visibility")
    val visibility: Int?,
    @SerializedName("wind")
    val wind: Wind?,
    @SerializedName("clouds")
    val clouds: Clouds?,
    @SerializedName("dt")
    val dt: Long?,
    @SerializedName("sys")
    val sys: Sys?,
    @SerializedName("timezone")
    val timezone: Int?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("name")
    val name: String?, // City name.
    @SerializedName("cod")
    val cod: Int? // Response code (e.g., 200 for OK).
)

data class Coord(
    @SerializedName("lon")
    val lon: Double?,
    @SerializedName("lat")
    val lat: Double?
)

data class WeatherDescription(
    @SerializedName("id")
    val id: Int?, // Weather condition ID.
    @SerializedName("main")
    val main: String?, // Weather parameter (e.g., Rain, Snow, Extreme).
    @SerializedName("description")
    val description: String?, // Weather condition description.
    @SerializedName("icon")
    val icon: String? // Weather icon ID.
)

data class WeatherMain(
    @SerializedName("temp")
    val temp: Double?, // Temperature. Default: Kelvin.
    @SerializedName("feels_like")
    val feelsLike: Double?,
    @SerializedName("temp_min")
    val tempMin: Double?,
    @SerializedName("temp_max")
    val tempMax: Double?,
    @SerializedName("pressure")
    val pressure: Int?,
    @SerializedName("humidity")
    val humidity: Int?
)

data class Wind(
    @SerializedName("speed")
    val speed: Double?,
    @SerializedName("deg")
    val deg: Int?
)

data class Clouds(
    @SerializedName("all")
    val all: Int? // Cloudiness percentage.
)

data class Sys(
    @SerializedName("type")
    val type: Int?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("sunrise")
    val sunrise: Long?,
    @SerializedName("sunset")
    val sunset: Long?
)
