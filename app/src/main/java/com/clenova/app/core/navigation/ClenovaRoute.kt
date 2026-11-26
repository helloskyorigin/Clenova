package com.clenova.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes for CLENOVA.
 */
sealed interface ClenovaRoute {
    @Serializable
    data object Home : ClenovaRoute

    @Serializable
    data object Clean : ClenovaRoute

    @Serializable
    data object Guard : ClenovaRoute

    @Serializable
    data object Agent : ClenovaRoute

    @Serializable
    data object Settings : ClenovaRoute

    @Serializable
    data object StorageDetails : ClenovaRoute

    @Serializable
    data object AutopilotSetup : ClenovaRoute

    @Serializable
    data object CleanupReview : ClenovaRoute

    @Serializable
    data object SmartScan : ClenovaRoute

    @Serializable
    data object Duplicates : ClenovaRoute

    @Serializable
    data object SimilarPhotos : ClenovaRoute

    @Serializable
    data object VideoCompress : ClenovaRoute
}

enum class PillDestination(
    val route: ClenovaRoute,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    HOME(
        route = ClenovaRoute.Home,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home",
    ),
    CLEAN(
        route = ClenovaRoute.Clean,
        labelRes = R.string.nav_clean,
        selectedIcon = Icons.Rounded.CleaningServices,
        unselectedIcon = Icons.Outlined.CleaningServices,
        testTag = "nav_tab_clean",
    ),
    GUARD(
        route = ClenovaRoute.Guard,
        labelRes = R.string.nav_guard,
        selectedIcon = Icons.Rounded.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_tab_guard",
    ),
}
