package com.sanelisolehlohla.weathernow.respository

import android.util.Log
import com.sanelisolehlohla.weathernow.Main
import com.sanelisolehlohla.weathernow.OneCallResponse
import com.sanelisolehlohla.weathernow.Weather
import com.sanelisolehlohla.weathernow.WeatherAPIservice
import com.sanelisolehlohla.weathernow.database.WeatherDao
import com.sanelisolehlohla.weathernow.database.WeatherEntity
import com.sanelisolehlohla.weathernow.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(private val apiService: WeatherAPIservice,   private val weatherDao: WeatherDao) {

    // Fetch weather by coordinates (latitude, longitude)
    suspend fun getWeatherByCoordinates(lat: Double, lon: Double, apiKey: String) =
        apiService.getWeatherByCoordinates(lat, lon, apiKey, "metric")  // Celsius

    // Fetch weather by city name
    suspend fun getWeatherByCity(cityName: String, apiKey: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Attempt to fetch weather from API
                val response = apiService.getWeatherByCity(cityName, apiKey, "metric")
                response?.let {
                    // Cache the API response in Room
                    val weatherEntity = WeatherEntity(
                        cityName = it.name,
                        temperature = it.main.temp,
                        description = it.weather.first().description
                    )
                    weatherDao.insertWeather(weatherEntity)
                    Log.d("WeatherRepository", "Cached weather data for ${it.name}")
                }
                response
            } catch (e: Exception) {
                // Fallback to cached data if offline
                val cachedWeather = weatherDao.getWeatherByCity(cityName)
                cachedWeather?.let {
                    Log.d("WeatherRepository", "Retrieved cached data for ${it.cityName}")
                    // Convert cached data to WeatherResponse format
                    WeatherResponse(
                        name = it.cityName,
                        main = Main(it.temperature, 0),
                        weather = listOf(Weather(0, it.description)),
                        hourly = emptyList(),
                        daily = emptyList()
                    )
                }
            }
        }
    }// Celsius

    suspend fun getOneCallWeather(lat: Double, lon: Double, apiKey: String): OneCallResponse {
        return apiService.getOneCallWeather(lat, lon, apiKey)
    }
}



