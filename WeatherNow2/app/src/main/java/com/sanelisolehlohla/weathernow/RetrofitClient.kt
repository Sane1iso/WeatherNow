package com.sanelisolehlohla.weathernow

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // Create Retrofit instance
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create()) // Convert JSON to Kotlin object
        .build()

    // Create WeatherApiService instance
    val weatherApiService: WeatherAPIservice = retrofit.create(WeatherAPIservice::class.java)
}

