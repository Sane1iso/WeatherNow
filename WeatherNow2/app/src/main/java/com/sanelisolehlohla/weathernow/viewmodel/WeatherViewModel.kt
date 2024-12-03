package com.sanelisolehlohla.weathernow.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanelisolehlohla.weathernow.OneCallResponse
import com.sanelisolehlohla.weathernow.WeatherResponse
import com.sanelisolehlohla.weathernow.domain.util.Resource
import com.sanelisolehlohla.weathernow.respository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherViewModel(private val weatherRepository: WeatherRepository) : ViewModel() {

    private val _weatherData = MutableLiveData<Resource<WeatherResponse>>()
    val weatherData: LiveData<Resource<WeatherResponse>> = _weatherData
    val oneCallWeatherData: MutableLiveData<Resource<OneCallResponse>> = MutableLiveData()

    // Fetch weather based on coordinates (latitude, longitude)
    fun fetchWeather(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            _weatherData.value = Resource.Loading()  // Set the Loading state
            try {
                val weatherResponse = weatherRepository.getWeatherByCoordinates(lat, lon, apiKey)
                _weatherData.value = Resource.Success(weatherResponse)
            } catch (e: Exception) {
                _weatherData.value =
                    Resource.Error("Error fetching weather data: ${e.localizedMessage}")
            }
        }
    }

    // Fetch weather based on city name with caching fallback
    fun fetchWeatherByCity(cityName: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _weatherData.postValue(Resource.Loading())
            try {
                val weatherResponse = weatherRepository.getWeatherByCity(cityName, apiKey)
                if (weatherResponse != null) {
                    _weatherData.postValue(Resource.Success(weatherResponse))
                } else {
                    _weatherData.postValue(Resource.Error("No internet and no cached data available"))
                }
            } catch (e: Exception) {
                _weatherData.postValue(Resource.Error("Error fetching weather data: ${e.localizedMessage}"))
            }
        }
    }




    fun fetchOneCallWeather(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            oneCallWeatherData.postValue(Resource.Loading())
            try {
                val response = weatherRepository.getOneCallWeather(lat, lon, apiKey)
                oneCallWeatherData.postValue(Resource.Success(response))
            } catch (e: Exception) {
                oneCallWeatherData.postValue(Resource.Error("Failed to fetch weather data."))
            }
        }
    }
}
