package com.sanelisolehlohla.weathernow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class Search : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var cityImageView: ImageView
    private lateinit var backHomeButton: Button

    private val jsonApiUrl = "https://api.jsonbin.io/v3/b/66f4136ee41b4d34e4374ba6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize UI components
        searchEditText = findViewById(R.id.et_search_city)
        searchButton = findViewById(R.id.btn_search_city)
        cityImageView = findViewById(R.id.iv_city_image)
        backHomeButton = findViewById(R.id.btn_back_home)

        searchButton.setOnClickListener {
            // Get the city entered by the user
            val cityName = searchEditText.text.toString().trim()

            // If city is not empty, fetch the image URL and display it
            if (cityName.isNotEmpty()) {
                fetchCityImage(cityName)
            }
        }

        backHomeButton.setOnClickListener {
            // Navigate back to MainActivity with the city name
            val cityName = searchEditText.text.toString()
            if (cityName.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.EXTRA_CITY_NAME, cityName)
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Close SearchActivity and return to MainActivity
            }
        }
    }

    private fun fetchCityImage(cityName: String) {
        thread {
            try {
                // Fetch the JSON response from the API
                val jsonStr = URL(jsonApiUrl).readText()

                // Parse the JSON response
                val jsonResponse = JSONObject(jsonStr)
                val record = jsonResponse.getJSONObject("record")  // Access the "record" object
                val cities = record.getJSONArray("cities")  // Get the "cities" array

                var cityFound = false

                for (i in 0 until cities.length()) {
                    val city = cities.getJSONObject(i)
                    val name = city.getString("name")

                    if (name.equals(cityName, ignoreCase = true)) {
                        val imageUrl = city.getString("image_url")

                        // Update the UI with the image on the main thread
                        runOnUiThread {
                            Picasso.get().load(imageUrl).into(cityImageView)
                            Toast.makeText(this@Search, "$cityName found!", Toast.LENGTH_SHORT).show()
                        }
                        cityFound = true
                        break
                    }
                }

                if (!cityFound) {
                    // If the city is not found, show a specific error message
                    runOnUiThread {
                        Toast.makeText(
                            this@Search,
                            "City Image not found, Just major cities of South Africa",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()

                // Handle network or API errors
                runOnUiThread {
                    Toast.makeText(this@Search, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}