package com.sanelisolehlohla.weathernow.domain.weather

import androidx.annotation.DrawableRes
import com.sanelisolehlohla.weathernow.R


sealed class WeatherType(
    val weatherDesc: String,
    @DrawableRes val iconRes: Int
) {
    object ClearSky : WeatherType(
        weatherDesc = "Clear sky",
        iconRes = R.drawable.ic_sunny
    )
    object MainlyClear : WeatherType(
        weatherDesc = "Mainly clear",
        iconRes = R.drawable.ic_cloudy
    )
    object PartlyCloudy : WeatherType(
        weatherDesc = "Partly cloudy",
        iconRes = R.drawable.ic_cloudy
    )
    object Overcast : WeatherType(
        weatherDesc = "Overcast",
        iconRes = R.drawable.ic_cloudy
    )
    object Foggy : WeatherType(
        weatherDesc = "Foggy",
        iconRes = R.drawable.ic_very_cloudy
    )
    object LightDrizzle : WeatherType(
        weatherDesc = "Light drizzle",
        iconRes = R.drawable.ic_rainshower
    )
    object ModerateRain : WeatherType(
        weatherDesc = "Moderate rain",
        iconRes = R.drawable.ic_rainy
    )
    object SlightRain : WeatherType(
        weatherDesc = "Slight rain",
        iconRes = R.drawable.ic_rainy
    )
    object HeavyRain : WeatherType(
        weatherDesc = "Heavy rain",
        iconRes = R.drawable.ic_rainy
    )
    object SlightSnowFall : WeatherType(
        weatherDesc = "Slight snow fall",
        iconRes = R.drawable.ic_snowy
    )
    object ModerateSnowFall : WeatherType(
        weatherDesc = "Moderate snow fall",
        iconRes = R.drawable.ic_heavysnow
    )
    object HeavySnowFall : WeatherType(
        weatherDesc = "Heavy snow fall",
        iconRes = R.drawable.ic_heavysnow
    )
    object ModerateThunderstorm : WeatherType(
        weatherDesc = "Moderate thunderstorm",
        iconRes = R.drawable.ic_thunder
    )

    companion object {
        // Updated method for OpenWeatherMap weather codes
        fun fromWMO(code: Int): WeatherType {
            return when (code) {
                // Clear sky
                800 -> ClearSky

                // Clouds
                in 801..802 -> MainlyClear
                in 803..804 -> Overcast

                // Fog
                in 701..781 -> Foggy

                // Drizzle
                in 300..321 -> LightDrizzle

                // Rain
                in 500..531 -> when (code) {
                    500 -> SlightRain
                    501 -> ModerateRain
                    502, 503, 504 -> HeavyRain
                    else -> ModerateRain
                }

                // Snow
                in 600..622 -> when (code) {
                    600 -> SlightSnowFall
                    601 -> ModerateSnowFall
                    602 -> HeavySnowFall
                    else -> ModerateSnowFall
                }

                // Thunderstorm
                in 200..232 -> ModerateThunderstorm

                // Default case
                else -> ClearSky
            }
        }
    }
}
