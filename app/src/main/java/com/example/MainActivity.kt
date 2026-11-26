package com.example

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clenova.app.ClenovaApp
import com.clenova.app.ClenovaApplication
import com.clenova.app.core.designsystem.ClenovaTheme
import com.clenova.app.feature.splash.ClenovaSplashScreen
import com.clenova.app.feature.splash.SplashMotionMillis
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dependencies = application as ClenovaApplication
        val playIntro = savedInstanceState == null && ValueAnimator.areAnimatorsEnabled()

        setContent {
            var showSplash by remember { mutableStateOf(playIntro) }
            LaunchedEffect(Unit) {
                if (showSplash) {
                    delay(SplashMotionMillis.toLong())
                    showSplash = false
                }
            }

            Crossfade(targetState = showSplash, animationSpec = tween(220), label = "launch") { splash ->
                if (splash) {
                    ClenovaTheme { ClenovaSplashScreen() }
                } else {
                    ClenovaApp(
                        storageRepository = dependencies.storageRepository,
                        preferencesRepository = dependencies.preferencesRepository,
                    )
                }
            }
        }
    }
}
