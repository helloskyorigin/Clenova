package com.clenova.app.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clenova.app.core.designsystem.ClenovaMotion
import com.clenova.app.domain.repository.PreferencesRepository
import com.clenova.app.domain.repository.StorageRepository
import com.clenova.app.feature.agent.AgentScreen
import com.clenova.app.feature.clean.CleanScreen
import com.clenova.app.feature.guard.GuardScreen
import com.clenova.app.feature.home.HomeScreen
import com.clenova.app.feature.home.HomeViewModel
import com.clenova.app.feature.storage.StorageDetailsScreen
import com.clenova.app.feature.settings.SettingsScreen
import com.clenova.app.feature.settings.SettingsViewModel

fun NavDestination?.toClenovaRoute(): ClenovaRoute? = when {
    this == null -> null
    hasRoute<ClenovaRoute.Home>() -> ClenovaRoute.Home
    hasRoute<ClenovaRoute.Clean>() -> ClenovaRoute.Clean
    hasRoute<ClenovaRoute.Guard>() -> ClenovaRoute.Guard
    hasRoute<ClenovaRoute.Agent>() -> ClenovaRoute.Agent
    hasRoute<ClenovaRoute.Settings>() -> ClenovaRoute.Settings
    hasRoute<ClenovaRoute.StorageDetails>() -> ClenovaRoute.StorageDetails
    else -> null
}

fun NavHostController.navigateTopLevel(route: ClenovaRoute) {
    val current = currentBackStackEntry?.destination.toClenovaRoute()
    if (current == route) return

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ClenovaNavHost(
    navController: NavHostController,
    storageRepository: StorageRepository,
    preferencesRepository: PreferencesRepository,
    onSelectionHaptic: () -> Unit,
    modifier: Modifier = Modifier,
    showSampleHome: Boolean = false,
) {
    NavHost(
        navController = navController,
        startDestination = ClenovaRoute.Home,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = ClenovaMotion.quickTween()) },
        exitTransition = { fadeOut(animationSpec = ClenovaMotion.quickTween()) },
        popEnterTransition = { fadeIn(animationSpec = ClenovaMotion.quickTween()) },
        popExitTransition = { fadeOut(animationSpec = ClenovaMotion.quickTween()) },
    ) {
        composable<ClenovaRoute.Home> {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    storageRepository = storageRepository,
                    preferencesRepository = preferencesRepository,
                ),
            )
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            HomeScreen(
                uiState = uiState,
                showSampleHome = showSampleHome,
                onRefreshStorage = {
                    onSelectionHaptic()
                    viewModel.refreshStorage()
                },
                onOpenAgent = {
                    onSelectionHaptic()
                    navController.navigateTopLevel(ClenovaRoute.Agent)
                },
                onOpenStorage = {
                    onSelectionHaptic()
                    navController.navigate(ClenovaRoute.StorageDetails) { launchSingleTop = true }
                },
                onOpenClean = { onSelectionHaptic(); navController.navigateTopLevel(ClenovaRoute.Clean) },
                onOpenGuard = { onSelectionHaptic(); navController.navigateTopLevel(ClenovaRoute.Guard) },
                onOpenSettings = {
                    onSelectionHaptic()
                    navController.navigate(ClenovaRoute.Settings) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ClenovaRoute.Clean> {
            CleanScreen()
        }

        composable<ClenovaRoute.Guard> {
            GuardScreen()
        }

        composable<ClenovaRoute.Agent> {
            AgentScreen()
        }

        composable<ClenovaRoute.StorageDetails>(
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = ClenovaMotion.gentleSpring()) +
                    fadeIn(animationSpec = ClenovaMotion.quickTween())
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = ClenovaMotion.gentleSpring()) +
                    fadeOut(animationSpec = ClenovaMotion.quickTween())
            },
        ) {
            val model: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(storageRepository, preferencesRepository),
            )
            val state = model.uiState.collectAsStateWithLifecycle().value
            StorageDetailsScreen(
                storageState = state.storageState,
                showSampleHome = showSampleHome,
                onBack = { onSelectionHaptic(); navController.popBackStack() },
                onRefresh = { onSelectionHaptic(); model.refreshStorage() },
                onOpenClean = { onSelectionHaptic(); navController.navigateTopLevel(ClenovaRoute.Clean) },
            )
        }

        composable<ClenovaRoute.Settings>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = ClenovaMotion.gentleSpring(),
                ) + fadeIn(animationSpec = ClenovaMotion.quickTween())
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = ClenovaMotion.gentleSpring(),
                ) + fadeOut(animationSpec = ClenovaMotion.quickTween())
            },
            popEnterTransition = { fadeIn(animationSpec = ClenovaMotion.quickTween()) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = ClenovaMotion.gentleSpring(),
                ) + fadeOut(animationSpec = ClenovaMotion.quickTween())
            },
        ) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    preferencesRepository = preferencesRepository,
                ),
            )
            val preferences = viewModel.preferences.collectAsStateWithLifecycle().value
            SettingsScreen(
                preferences = preferences,
                onSelectTheme = { theme ->
                    onSelectionHaptic()
                    viewModel.updateTheme(theme)
                },
                onToggleHaptics = { enabled ->
                    viewModel.updateHapticsEnabled(enabled)
                },
                onNavigateBack = {
                    onSelectionHaptic()
                    navController.popBackStack()
                },
            )
        }
    }
}
