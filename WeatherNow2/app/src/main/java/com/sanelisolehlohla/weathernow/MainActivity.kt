package com.sanelisolehlohla.weathernow

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sanelisolehlohla.weathernow.domain.util.Resource
import com.sanelisolehlohla.weathernow.domain.weather.WeatherType
import com.sanelisolehlohla.weathernow.respository.WeatherRepository
import com.sanelisolehlohla.weathernow.viewmodel.WeatherViewModel
import com.sanelisolehlohla.weathernow.viewmodel.WeatherViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import androidx.room.Room
import com.sanelisolehlohla.weathernow.database.NetworkUtil
import com.sanelisolehlohla.weathernow.database.WeatherDao
import com.sanelisolehlohla.weathernow.database.WeatherDatabase
import com.sanelisolehlohla.weathernow.database.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var viewModel: WeatherViewModel? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar

    private lateinit var cityNameTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var tempFahrenheitTextView: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var weatherDescTextView: TextView
    private lateinit var backgroundLayout: RelativeLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var navigationView: NavigationView

    private lateinit var sharedPreferences: SharedPreferences
    private var isCelsius: Boolean = true // Default to Celsius
    private lateinit var weatherDao: WeatherDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        fetchWeatherDataForCity("LastCitySearched")


        // Initialize UI components
        cityNameTextView = findViewById(R.id.tv_city_name)
        tempTextView = findViewById(R.id.tv_temperature)
        tempFahrenheitTextView = findViewById(R.id.tv_temperature_fahrenheit)
        weatherIcon = findViewById(R.id.iv_weather_icon)
        weatherDescTextView = findViewById(R.id.tv_weather_description)

        // Set up SharedPreferences
        sharedPreferences = getSharedPreferences("WeatherPrefs", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        isCelsius = sharedPreferences.getBoolean("isCelsius", true)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Set up periodic work for notifications
        val weatherWorkRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueue(weatherWorkRequest)

        // Initialize search button
        findViewById<FloatingActionButton>(R.id.fab_search).setOnClickListener {
            startActivityForResult(Intent(this, Search::class.java), REQUEST_CODE_SEARCH)
        }

        // Initialize Toolbar and DrawerLayout
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Update navigation menu based on login state
        updateNavigationMenu()

        // Initialize Retrofit and ViewModel
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()


        val db = WeatherDatabase.getDatabase(applicationContext)
        weatherDao = db.weatherDao()

        // Initialize Retrofit and ViewModel
        val apiService = retrofit.create(WeatherAPIservice::class.java)
        val weatherRepository = WeatherRepository(apiService, weatherDao)

        val factory = WeatherViewModelFactory(weatherRepository)
        viewModel = ViewModelProvider(this, factory).get(WeatherViewModel::class.java)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location and show weather
        getLocationAndShowWeather()

        // Observe weather data
        viewModel?.weatherData?.observe(this, Observer { resource ->
            when (resource) {
                is Resource.Success -> {
                    val weather = resource.data
                    if (weather != null) {
                        cityNameTextView.text = weather.name ?: "N/A"
                        updateTemperatureDisplay(weather.main?.temp?.toInt() ?: 0)
                        val weatherType = WeatherType.fromWMO(weather.weather?.firstOrNull()?.id ?: 0)
                        weatherDescTextView.text = weatherType?.weatherDesc ?: "Unknown"
                        weatherIcon.setImageResource(weatherType?.iconRes ?: R.drawable.default_weather_icon)

                        // Save data to database
                        saveWeatherToDatabase(weather)
                    }
                }
                is Resource.Error -> {
                    cityNameTextView.text = "Error: ${resource.message}"
                    tempTextView.text = ""
                    tempFahrenheitTextView.text = ""
                    weatherDescTextView.text = ""
                    weatherIcon.setImageResource(R.drawable.default_weather_icon)

                    // Load cached weather data
                    loadCachedWeatherData(resource.data?.name ?: "DefaultCity")
                }
                is Resource.Loading -> {
                    cityNameTextView.text = "Loading..."
                    tempTextView.text = ""
                    tempFahrenheitTextView.text = ""
                    weatherDescTextView.text = ""
                    weatherIcon.setImageResource(R.drawable.default_weather_icon)
                }
            }
        })
    }


private fun fetchWeatherDataForCity(cityName: String) {
    if (NetworkUtil.isNetworkAvailable(this)) {
        // Online: Fetch data from the API
        viewModel?.fetchWeatherByCity(cityName, "8ec1a67051dbccce54fd9c1b7a739b1b")
    } else {
        // Offline: Fetch data from the Room database
        loadCachedWeatherData(cityName)
        Toast.makeText(this, "No internet connection. Displaying cached data.", Toast.LENGTH_LONG).show()
    }
}
    private fun updateUIWithWeatherData(weatherEntity: WeatherEntity) {
        cityNameTextView.text = weatherEntity.cityName
        updateTemperatureDisplay(weatherEntity.temperature.toInt())
        weatherDescTextView.text = weatherEntity.description

        // Optionally set a default icon if necessary
        weatherIcon.setImageResource(R.drawable.default_weather_icon)
    }

    private fun saveWeatherToDatabase(weather: WeatherResponse) {
        // Create a WeatherEntity from the WeatherResponse
        val weatherEntity = WeatherEntity(
            cityName = weather.name,
            temperature = weather.main?.temp ?: 0.0,
            description = weather.weather?.firstOrNull()?.description ?: ""
        )
        // Save in a coroutine using lifecycleScope and Dispatchers.IO
        lifecycleScope.launch(Dispatchers.IO) {
            weatherDao.insertWeather(weatherEntity)
        }
    }

//for offline use  get cahced weatherdata
private fun loadCachedWeatherData(cityName: String) {
    lifecycleScope.launch(Dispatchers.IO) { // Use IO dispatcher for database operations
        try {
            val weatherEntity = weatherDao.getWeatherByCity(cityName)
            if (weatherEntity != null) {
                withContext(Dispatchers.Main) { // Switch back to Main thread to update UI
                    updateUIWithWeatherData(weatherEntity)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "No cached data found for $cityName", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error fetching cached data", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }
}



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if the request code matches the search request
        if (requestCode == REQUEST_CODE_SEARCH && resultCode == Activity.RESULT_OK) {
            // Get the city name from the result
            val cityName = data?.getStringExtra(EXTRA_CITY_NAME)

            // If a city name was returned, update the UI and fetch the weather for that city
            if (!cityName.isNullOrEmpty()) {
                cityNameTextView.text = cityName
                // Optionally fetch weather data for the searched city
                viewModel?.fetchWeatherByCity(cityName, "8ec1a67051dbccce54fd9c1b7a739b1b") // API key
            }
        }
    }


    private fun updateTemperatureDisplay(tempCelsius: Int) {
        if (isCelsius) {
            tempTextView.text = "$tempCelsius°C"
            tempFahrenheitTextView.text = "${convertToFahrenheit(tempCelsius)}°F"
        } else {
            tempTextView.text = "${convertToFahrenheit(tempCelsius)}°F"
            tempFahrenheitTextView.text = "$tempCelsius°C"
        }
    }

    private fun convertToFahrenheit(celsius: Int): String {
        val fahrenheit = (celsius * 9 / 5) + 32
        return fahrenheit.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_temp -> {
                toggleTemperatureUnit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTemperatureUnit() {
        isCelsius = !isCelsius
        sharedPreferences.edit().putBoolean("isCelsius", isCelsius).apply()
        viewModel?.weatherData?.value?.data?.main?.temp?.toInt()?.let { updateTemperatureDisplay(it) }
    }

    private fun getLocationAndShowWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplanationDialog()
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return
        }

        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                viewModel?.fetchWeather(location.latitude, location.longitude, "8ec1a67051dbccce54fd9c1b7a739b1b")
            } else {
                cityNameTextView.text = "Location not available"
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("This app needs location permission to fetch weather data based on your location.")
            .setPositiveButton("OK") { _, _ ->
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .create()
            .show()
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocationAndShowWeather()
            } else {
                cityNameTextView.text = "Permission Denied"
            }
        }

    private fun updateNavigationMenu() {
        val menu = navigationView.menu
        val isLoggedIn = auth.currentUser != null

        menu.findItem(R.id.nav_sign_up).isVisible = !isLoggedIn
        menu.findItem(R.id.nav_login).isVisible = !isLoggedIn
        menu.findItem(R.id.nav_edit_profile).isVisible = isLoggedIn
        menu.findItem(R.id.nav_logout).isVisible = isLoggedIn
    }

    // Handle navigation item clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_sign_up -> {
// Navigate to Sign Up screen
                startActivity(Intent(this, SignUpActivity::class.java))
            }
            R.id.nav_login -> {
// Navigate to Login screen
                startActivity(Intent(this, LoginActivity::class.java))
            }
            R.id.nav_edit_profile -> {
// Navigate to Edit Profile screen
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            R.id.nav_settings -> {
// Navigate to Settings screen
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_logout -> {
// Log the user out and update menu
                auth.signOut()
                updateNavigationMenu()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    companion object {
        const val REQUEST_CODE_SEARCH = 1
        const val EXTRA_CITY_NAME = "extra_city_name"
    }
}
