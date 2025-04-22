package com.example.finessa.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.finessa.MainActivity // Correct path to MainActivity
import com.example.finessa.R

@SuppressLint("CustomSplashScreen") // Suppress Lint warning for custom splash screen
class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000 // Time in milliseconds (2 seconds)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Assuming you have activity_splash.xml

        Handler(Looper.getMainLooper()).postDelayed({
            // Start your app main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close this activity
            finish()
        }, SPLASH_TIME_OUT)
    }
}