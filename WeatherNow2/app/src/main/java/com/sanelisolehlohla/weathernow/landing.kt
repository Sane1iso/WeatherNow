package com.sanelisolehlohla.weathernow

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sanelisolehlohla.weathernow.R

class landing: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        // Create a delay of 2 seconds (2000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Intent to start the MainActivity after the delay
            val intent = Intent(this@landing, MainActivity::class.java)
            startActivity(intent)
            finish() // Close LandingActivity
        }, 2000) // 2000 milliseconds = 2 seconds
    }
}
