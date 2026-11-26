package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clenova.app.ClenovaApp
import com.clenova.app.ClenovaApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dependencies = application as ClenovaApplication
        // Do not block startup on disk IO. HomeViewModel loads storage asynchronously.
        setContent {
            ClenovaApp(
                storageRepository = dependencies.storageRepository,
                preferencesRepository = dependencies.preferencesRepository,
            )
        }
    }
}
