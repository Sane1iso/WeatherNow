package com.sanelisolehlohla.weathernow

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val CHANNEL_ID = "WeatherNowChannel"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved language before setting content view

        setContentView(R.layout.activity_settings)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val themeSwitch = findViewById<Switch>(R.id.switch_theme)
        val notificationSwitch = findViewById<Switch>(R.id.switch_notifications)
        val unitSpinner = findViewById<Spinner>(R.id.spinner_unit)
        val languageSpinner = findViewById<Spinner>(R.id.spinner_language)

        // Set up the Spinner with Celsius and Fahrenheit options
        val unitOptions = arrayOf("Celsius (°C)", "Fahrenheit (°F)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter

        // Set up the language Spinner with Afrikaans and Zulu options
        val languageOptions = arrayOf(getString(R.string.English), getString(R.string.Afrikaans), getString(
            R.string.Zulu
        ))
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageOptions)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter

        // Load the saved language or default to English (strings.xml language)
        val savedLanguage = sharedPreferences.getString("selected_language", "en")
        val defaultPosition = when (savedLanguage) {
            "af" -> 1
            "zu" -> 2
            else -> 0  // Default to English
        }
        languageSpinner.setSelection(defaultPosition)



        // Load settings for theme, notifications, and unit
        if (auth.currentUser != null) {
            loadSettingsFromFirebase()
        } else {
            loadSettingsFromPreferences()
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Save the setting locally or in Firebase
            if (auth.currentUser != null) {
                saveSettingsToFirebase(isChecked)
            } else {
                saveSettingsToPreferences(isChecked)
            }
        }


        // Handle notification switch
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        checkAndRequestLocationPermission()
                    }
                } else {
                    checkAndRequestLocationPermission()
                }
            } else {
                disableWeatherNotification()
            }
        }

        // Handle unit selection
        unitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedUnit = if (position == 0) "metric" else "imperial"
                saveUnitPreference(selectedUnit)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle the case when nothing is selected
            }
        }
        // Update the language spinner listener to handle English as well
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = when (position) {
                    1 -> "af"  // Afrikaans
                    2 -> "zu"  // Zulu
                    else -> "en"  // English (default)
                }
                setLocale(selectedLanguage)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        createNotificationChannel()
    }
    fun setLocale(localeName: String) {
        // Retrieve the current locale saved in SharedPreferences
        val savedLanguage = sharedPreferences.getString("selected_language", Locale.getDefault().language)

        // Only change the locale if the selected language is different from the current one
        if (savedLanguage != localeName) {
            val locale = Locale(localeName)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Save the selected language to SharedPreferences
            sharedPreferences.edit().putString("selected_language", localeName).apply()

            // Recreate the activity to apply the language change
            recreate()
        }
    }


    private fun loadSettingsFromFirebase() {
        val database = FirebaseDatabase.getInstance().reference
        val user = auth.currentUser

        user?.let {
            database.child("users").child(user.uid).child("settings").get().addOnSuccessListener { snapshot ->
                val isDarkMode = snapshot.child("dark_mode").getValue(Boolean::class.java) ?: false
                val isNotificationsEnabled = snapshot.child("notifications").getValue(Boolean::class.java) ?: false
                val unit = snapshot.child("unit").getValue(String::class.java) ?: "metric"

                findViewById<Switch>(R.id.switch_theme).isChecked = isDarkMode
                findViewById<Switch>(R.id.switch_notifications).isChecked = isNotificationsEnabled
                findViewById<Spinner>(R.id.spinner_unit).setSelection(if (unit == "metric") 0 else 1)
            }
        }
    }

    private fun loadSettingsFromPreferences() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications", false)
        val unit = sharedPreferences.getString("unit", "metric")

        findViewById<Switch>(R.id.switch_theme).isChecked = isDarkMode
        findViewById<Switch>(R.id.switch_notifications).isChecked = isNotificationsEnabled
        findViewById<Spinner>(R.id.spinner_unit).setSelection(if (unit == "metric") 0 else 1)
    }

    private fun saveSettingsToFirebase(isChecked: Boolean) {
        // Update local preferences immediately
        val editor = sharedPreferences.edit()
        editor.putBoolean("dark_mode", isChecked)
        editor.apply()

        // Save to Firebase
        val database = FirebaseDatabase.getInstance().reference
        val user = auth.currentUser
        user?.let {
            val userSettings = mapOf(
                "dark_mode" to isChecked,
                "notifications" to findViewById<Switch>(R.id.switch_notifications).isChecked,
                "unit" to (if ( findViewById<Spinner>(R.id.spinner_unit).selectedItemPosition == 0) "metric" else "imperial")
            )
            database.child("users").child(user.uid).child("settings").setValue(userSettings)
                .addOnSuccessListener {
                    Toast.makeText(this, "Settings saved to profile", Toast.LENGTH_SHORT).show()
                }

        }
    }

    private fun saveSettingsToPreferences(isChecked: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("dark_mode", isChecked)
        editor.putBoolean("notifications", findViewById<Switch>(R.id.switch_notifications).isChecked)
        editor.putString(
            "unit",
            if (findViewById<Spinner>(R.id.spinner_unit).selectedItemPosition == 0) "metric" else "imperial"
        )
        editor.apply()
        Toast.makeText(this, "Settings saved locally", Toast.LENGTH_SHORT).show()
    }

    private fun saveUnitPreference(selectedUnit: String) {
        val editor = sharedPreferences.edit()
        editor.putString("unit", selectedUnit)
        editor.apply()

        if (auth.currentUser != null) {
            saveSettingsToFirebase(findViewById<Switch>(R.id.switch_theme).isChecked)
        }
    }

    // Notification Permission Request (for Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkAndRequestLocationPermission()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Location Permission Request
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableWeatherNotification()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            enableWeatherNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun enableWeatherNotification() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    fetchWeatherAndNotify(location.latitude, location.longitude)
                }
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableWeatherNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(1)
    }

    private suspend fun fetchWeatherAndNotify(latitude: Double, longitude: Double) {
        val selectedUnit = sharedPreferences.getString("unit", "metric")
        val unitSymbol = if (selectedUnit == "metric") "°C" else "°F"

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(WeatherAPIservice::class.java)
        val weatherResponse = withContext(Dispatchers.IO) {
            apiService.getWeatherByCoordinates(latitude, longitude, "8ec1a67051dbccce54fd9c1b7a739b1b", selectedUnit ?: "metric")
        }

        weatherResponse?.let {
            showWeatherNotification(
                "Current Weather",
                "${it.weather.first().description}, ${it.main.temp.toInt()}$unitSymbol"
            )
        }
    }

    private fun showWeatherNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
}
