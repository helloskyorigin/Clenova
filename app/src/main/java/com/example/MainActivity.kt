package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.clenova.app.ClenovaApp
import com.clenova.app.core.storage.DeviceStorageProvider
import com.clenova.app.data.preferences.DataStorePreferencesRepository
import com.clenova.app.data.storage.LocalStorageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isFoundationReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isFoundationReady }

        val appContext = applicationContext
        val preferencesRepository = DataStorePreferencesRepository(appContext)
        val storageRepository = LocalStorageRepository(DeviceStorageProvider(appContext))

        lifecycleScope.launch {
            runCatching { preferencesRepository.userPreferences.first() }
            storageRepository.refreshStorage()
            isFoundationReady = true
        }

        enableEdgeToEdge()
        setContent {
            ClenovaApp(
                storageRepository = storageRepository,
                preferencesRepository = preferencesRepository,
            )
        }
    }
}
