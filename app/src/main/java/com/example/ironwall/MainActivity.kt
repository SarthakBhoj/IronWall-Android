package com.example.ironwall

import GlobalStatusObserver
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.ironwall.ui.theme.IronWallTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val httpClient = HttpClient(Android) { }

        setContent {
            IronWallTheme {
                GlobalStatusObserver(activity = this) {
                    Log.d("Inside global state", "ABCD")
                    AppNavigation(httpClient)
                }
            }
        }
    }
}