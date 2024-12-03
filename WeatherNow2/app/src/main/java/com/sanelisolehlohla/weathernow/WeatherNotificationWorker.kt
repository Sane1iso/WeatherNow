package com.sanelisolehlohla.weathernow

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sanelisolehlohla.weathernow.domain.weather.WeatherType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherNotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val CHANNEL_ID = "WeatherNotifications"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val WEATHER_API_KEY = "8ec1a67051dbccce54fd9c1b7a739b1b"

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun doWork(): Result {
        // Check location permission before accessing location
        return if (hasLocationPermission()) {
            // Fetch weather data and show notification only if permission granted
            fetchWeatherAndShowNotification()
        } else {
            // Handle permission rejection gracefully
            Result.failure()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun fetchWeatherAndShowNotification(): Result {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(WeatherAPIservice::class.java)

        try {
            // Get last known location only if permission granted
            if (hasLocationPermission()) {
                val location = try {
                    fusedLocationClient.lastLocation.await()
                } catch (e: SecurityException) {
                    // Handle security exception if permission was denied in the past
                    return Result.failure()
                }

                val latitude = location?.latitude ?: return Result.failure()
                val longitude = location?.longitude ?: return Result.failure()

                // Fetch weather data
                val weatherResponse = apiService.getWeatherByCoordinates(latitude, longitude, WEATHER_API_KEY)
                val weatherType = WeatherType.fromWMO(weatherResponse.weather.first().id)
                val cityName = weatherResponse.name ?: "Unknown Location"

                // Show notification, only if we have notification permission
                if (hasNotificationPermission()) {
                    showNotification(cityName, weatherType.weatherDesc, weatherType.iconRes)
                } else {
                    return Result.failure()
                }

                return Result.success()
            } else {
                return Result.failure()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check notification permission on Android 13 (API level 33) and above
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android versions below 13, no need to check notification permission
            true
        }
    }

    private fun showNotification(cityName: String, weatherDescription: String, weatherIconRes: Int) {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(weatherIconRes) // Weather icon
            .setContentTitle("Weather in $cityName")
            .setContentText(weatherDescription) // Weather description
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true) // Silent notification

        with(NotificationManagerCompat.from(applicationContext)) {
            // Check notification permission again before displaying notification
            if (hasNotificationPermission()) {
                notify(1, builder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Notification Channel"
            val descriptionText = "Channel for weather notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
