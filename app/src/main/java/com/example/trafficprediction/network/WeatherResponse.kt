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
    val name: String?, // Şehir adı
    @SerializedName("cod")
    val cod: Int? // Yanıt kodu (200 OK gibi)
)

data class Coord(
    @SerializedName("lon")
    val lon: Double?,
    @SerializedName("lat")
    val lat: Double?
)

data class WeatherDescription(
    @SerializedName("id")
    val id: Int?, // Hava durumu condition ID
    @SerializedName("main")
    val main: String?, // Hava durumu parametresi (Rain, Snow, Extreme etc.)
    @SerializedName("description")
    val description: String?, // Hava durumu açıklaması
    @SerializedName("icon")
    val icon: String? // Hava durumu ikon ID'si
)

data class WeatherMain(
    @SerializedName("temp")
    val temp: Double?, // Sıcaklık. Varsayılan: Kelvin
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
    val all: Int? // Bulutluluk yüzdesi
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
