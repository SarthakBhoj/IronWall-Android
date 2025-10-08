package com.example.ironwall

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import com.example.ironwall.ui.theme.IronWallTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen (edge-to-edge layout)
        enableEdgeToEdge()

        // Disable screenshots & screen recording for security
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Initialize your secure HTTP client
        val httpClient = HttpClient(Android) {
            // Optional: Add configuration here for logging, timeouts, or SSL pinning
        }

        setContent {
            IronWallTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing // Handles status & nav bars
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // ✅ Pass HttpClient to navigation
                        AppNavigation(httpClient = httpClient)
                    }
                }
            }
        }
    }
}
