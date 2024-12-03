package com.sanelisolehlohla.weathernow

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("name") val name: String,  // City name
    @SerializedName("main") val main: Main,    // Main weather data
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("hourly") val hourly: List<HourlyWeather>,
    @SerializedName("daily") val daily: List<DailyWeather>
)

data class Main(
    @SerializedName("temp") val temp: Double,  // Temperature
    @SerializedName("humidity") val humidity: Int
)

data class Weather(
    @SerializedName("id") val id: Int, // Weather condition code (for icon mapping)
    @SerializedName("description") val description: String

)
data class OneCallResponse(
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("hourly") val hourly: List<HourlyWeather>,
    @SerializedName("daily") val daily: List<DailyWeather>
)

data class CurrentWeather(
    @SerializedName("temp") val temp: Double,
    @SerializedName("weather") val weather: List<Weather>
)

data class HourlyWeather(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("time") val time: String,
    @SerializedName("weatherCode") val weatherCode: Int,
    @SerializedName("temp") val temp: Double,
    @SerializedName("weather") val weather: List<Weather>
)

data class DailyWeather(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("temp") val temp: Temp,
    @SerializedName("day") val day: String,
    @SerializedName("weatherCode") val weatherCode: Int,
    @SerializedName("weather") val weather: List<Weather>
)

data class Temp(
    @SerializedName("day") val day: Double,
    @SerializedName("night") val night: Double
)

