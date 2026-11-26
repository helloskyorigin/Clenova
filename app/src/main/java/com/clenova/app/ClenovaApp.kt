package com.clenova.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clenova.app.core.designsystem.ClenovaMotion
import com.clenova.app.core.designsystem.ClenovaTheme
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.navigation.ClenovaFloatingNavBar
import com.clenova.app.core.navigation.ClenovaNavHost
import com.clenova.app.core.navigation.ClenovaRoute
import com.clenova.app.core.navigation.navigateTopLevel
import com.clenova.app.core.navigation.toClenovaRoute
import com.clenova.app.core.utilities.rememberClenovaHaptics
import com.clenova.app.domain.model.UserPreferences
import com.clenova.app.domain.repository.PreferencesRepository
import com.clenova.app.domain.repository.StorageRepository

@Composable
fun ClenovaApp(
    storageRepository: StorageRepository,
    preferencesRepository: PreferencesRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    showSampleHome: Boolean = false,
) {
    val preferences by preferencesRepository.userPreferences
        .collectAsStateWithLifecycle(initialValue = UserPreferences())

    val haptics = rememberClenovaHaptics(enabled = preferences.hapticsEnabled)

    ClenovaTheme(themePreference = preferences.themePreference) {
        val colors = ClenovaThemeTokens.colors
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination.toClenovaRoute()
        val view = LocalView.current
        DisposableEffect(currentRoute, view) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            if (currentRoute == null || currentRoute == ClenovaRoute.Home || currentRoute == ClenovaRoute.StorageDetails || currentRoute == ClenovaRoute.AutopilotSetup || currentRoute == ClenovaRoute.CleanupReview || currentRoute == ClenovaRoute.SmartScan || currentRoute == ClenovaRoute.Duplicates || currentRoute == ClenovaRoute.SimilarPhotos || currentRoute == ClenovaRoute.Screenshots || currentRoute == ClenovaRoute.LargeFiles || currentRoute == ClenovaRoute.LargeVideos || currentRoute == ClenovaRoute.VideoCompress || currentRoute == ClenovaRoute.Guard || currentRoute == ClenovaRoute.Settings) {
                controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller?.show(WindowInsetsCompat.Type.statusBars())
            }
            onDispose { controller?.show(WindowInsetsCompat.Type.statusBars()) }
        }
        val showFloatingNav = currentRoute != ClenovaRoute.Settings && currentRoute != ClenovaRoute.StorageDetails && currentRoute != ClenovaRoute.AutopilotSetup && currentRoute != ClenovaRoute.CleanupReview && currentRoute != ClenovaRoute.SmartScan && currentRoute != ClenovaRoute.Duplicates && currentRoute != ClenovaRoute.SimilarPhotos && currentRoute != ClenovaRoute.Screenshots && currentRoute != ClenovaRoute.LargeFiles && currentRoute != ClenovaRoute.LargeVideos && currentRoute != ClenovaRoute.VideoCompress

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            ClenovaNavHost(
                navController = navController,
                storageRepository = storageRepository,
                preferencesRepository = preferencesRepository,
                onSelectionHaptic = { haptics.performSelection() },
                showSampleHome = showSampleHome,
            )

            AnimatedVisibility(
                visible = showFloatingNav,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = ClenovaMotion.snappySpring(),
                ) + fadeIn(animationSpec = ClenovaMotion.quickTween()),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = ClenovaMotion.snappySpring(),
                ) + fadeOut(animationSpec = ClenovaMotion.quickTween()),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ClenovaFloatingNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == ClenovaRoute.Agent) {
                            haptics.performPrimaryAction()
                        } else {
                            haptics.performSelection()
                        }
                        navController.navigateTopLevel(route)
                    },
                )
            }
        }
    }
}
