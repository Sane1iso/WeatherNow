
package com.sanelisolehlohla.weathernow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE cityName = :cityName LIMIT 1")
     fun getWeatherByCity(cityName: String): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertWeather(weather: WeatherEntity): Long
}
