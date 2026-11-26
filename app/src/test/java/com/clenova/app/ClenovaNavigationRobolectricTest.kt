package com.clenova.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.clenova.app.domain.model.StorageOverview
import com.clenova.app.domain.model.StorageState
import com.clenova.app.domain.model.ThemePreference
import com.clenova.app.domain.model.UserPreferences
import com.clenova.app.domain.repository.PreferencesRepository
import com.clenova.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ClenovaNavigationRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeStorageRepository(
        initialState: StorageState = StorageState.Ready(
            StorageOverview(
                totalBytes = 128_000_000_000L,
                availableBytes = 42_500_000_000L,
                usedBytes = 85_500_000_000L,
            ),
        ),
    ) : StorageRepository {
        private val _state = MutableStateFlow(initialState)
        override val storageState: StateFlow<StorageState> = _state.asStateFlow()
        override suspend fun refreshStorage() = Unit
    }

    private class FakePreferencesRepository : PreferencesRepository {
        private val _prefs = MutableStateFlow(UserPreferences())
        override val userPreferences: Flow<UserPreferences> = _prefs.asStateFlow()
        override suspend fun setThemePreference(preference: ThemePreference) {
            _prefs.value = _prefs.value.copy(themePreference = preference)
        }

        override suspend fun setAutopilotPreferences(goalGb: Int, intervalDays: Int) {
            _prefs.value = _prefs.value.copy(storageGoalGb = goalGb, checkIntervalDays = intervalDays)
        }

        override suspend fun resetPreferences() { _prefs.value = UserPreferences() }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            _prefs.value = _prefs.value.copy(hapticsEnabled = enabled)
        }
    }

    @Test
    fun `navigates across 3 plus 1 destinations and hides bottom nav on settings`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(),
                preferencesRepository = FakePreferencesRepository(),
            )
        }
        composeTestRule.waitForIdle()

        // Home renders branding, real storage summary, and honest inactive agent card
        composeTestRule.onNodeWithText("clenova").assertIsDisplayed()
        composeTestRule.onNodeWithText("42.5 GB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Not activated").performScrollTo().assertIsDisplayed()

        // Storage opens a secondary screen without the floating navigation.
        composeTestRule.onNodeWithTag("home_storage_details").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("storage_available_value").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()
        composeTestRule.onNodeWithTag("storage_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()

        // Home shortcut opens the full manual-tool workspace
        composeTestRule.onNodeWithTag("home_view_all").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Duplicates").assertIsDisplayed()

        // Navigate to Guard
        composeTestRule.onNodeWithTag("nav_tab_guard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Memory Guard").assertIsDisplayed()

        // Navigate to Agent via 3+1 circular button
        composeTestRule.onNodeWithTag("nav_tab_agent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agent Workspace").assertIsDisplayed()

        // Return to Home and open Settings
        composeTestRule.onNodeWithTag("nav_tab_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()

        // Settings renders and hides bottom nav
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()

        // Appearance uses a compact menu, not permanently visible theme chips.
        composeTestRule.onNodeWithTag("theme_option_dark").assertDoesNotExist()
        composeTestRule.onNodeWithTag("settings_appearance").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("theme_option_dark").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_option_dark").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()

        // Back from Settings restores Home and bottom nav
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
    }

    @Test
    fun `debug design state is isolated from device storage`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(StorageState.Unavailable),
                preferencesRepository = FakePreferencesRepository(),
                showSampleHome = true,
            )
        }
        composeTestRule.waitForIdle()
        if (com.example.BuildConfig.DEBUG) {
            composeTestRule.onNodeWithText("24.6 GB").assertIsDisplayed()
            composeTestRule.onNodeWithText("4.8 GB").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("home_autopilot_switch").performScrollTo().performClick()
            composeTestRule.onNodeWithText("Activate Autopilot").performScrollTo().performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("auto_goal_10").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("auto_interval_3").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("auto_setup_save").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("4.8 GB").performScrollTo().assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("Storage details unavailable").assertIsDisplayed()
        }
    }

    @Test
    fun `debug cleanup review changes estimated space without deleting files`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(),
                preferencesRepository = FakePreferencesRepository(),
                showSampleHome = true,
            )
        }
        composeTestRule.waitForIdle()
        if (com.example.BuildConfig.DEBUG) {
            composeTestRule.onNodeWithTag("open_agent_workspace_button").performScrollTo().performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("4.8 GB").assertIsDisplayed()
            composeTestRule.onNodeWithTag("review_group_4").performScrollTo().performClick()
            composeTestRule.onNodeWithText("4.3 GB").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("review_approve").performClick()
            composeTestRule.onNodeWithTag("review_confirm").performClick()
            composeTestRule.onNodeWithText("Plan approved").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("review_approve").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Plan approved").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `smart scan opens from home and does not fabricate release results`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(),
                preferencesRepository = FakePreferencesRepository(),
                showSampleHome = false,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_smart_scan").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Smart Scan is not ready yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_result_total").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()
        composeTestRule.onNodeWithTag("scan_back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
    }

    @Test
    fun `duplicates opens from home with no fabricated release results`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(),
                preferencesRepository = FakePreferencesRepository(),
                showSampleHome = false,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_duplicates").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Duplicate scanning is not available yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("duplicates_total").assertDoesNotExist()
        composeTestRule.onNodeWithTag("duplicates_back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
    }

    @Test
    fun `compress opens from home without fake release results`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(),
                preferencesRepository = FakePreferencesRepository(),
                showSampleHome = false,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_compress").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Video compression is not available yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("compress_back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
    }

    @Test
    fun `guard selection updates protected count on home`() {
        composeTestRule.setContent {
            ClenovaApp(storageRepository = FakeStorageRepository(), preferencesRepository = FakePreferencesRepository(), showSampleHome = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_guard").performClick()
        composeTestRule.waitForIdle()
        if (com.example.BuildConfig.DEBUG) {
            composeTestRule.onNodeWithText("248").assertIsDisplayed()
            composeTestRule.onNodeWithTag("guard_add").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("guard_pick_16").performClick()
            composeTestRule.onNodeWithTag("guard_add_confirm").performClick()
            composeTestRule.onNodeWithText("266").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("nav_tab_home").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("266 items protected").performScrollTo().assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("No protected collections yet").assertIsDisplayed()
        }
    }

    @Test
    fun `privacy dialogs and reset use real preference interface`() {
        val prefs = FakePreferencesRepository()
        composeTestRule.setContent { ClenovaApp(storageRepository = FakeStorageRepository(), preferencesRepository = prefs) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_permissions").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("permissions_manage").assertIsDisplayed()
        composeTestRule.onNodeWithTag("permissions_back").performClick()
        composeTestRule.onNodeWithTag("settings_privacy").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("privacy_back").performClick()
        composeTestRule.onNodeWithTag("settings_appearance").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("theme_option_dark").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_reset").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("settings_reset_confirm").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_reset_done").performClick()
        composeTestRule.onNodeWithTag("settings_appearance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
    }

    @Test
    fun `support and legal rows open actual content`() {
        composeTestRule.setContent { ClenovaApp(storageRepository = FakeStorageRepository(), preferencesRepository = FakePreferencesRepository()) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_help").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("help_search").assertIsDisplayed()
        composeTestRule.onNodeWithTag("help_contact").performClick()
        composeTestRule.onNodeWithTag("support_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("support_open_email").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.onNodeWithTag("settings_policy").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("legal_done").performClick()
        composeTestRule.onNodeWithTag("settings_terms").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("legal_done").performClick()
        composeTestRule.onNodeWithTag("settings_licenses").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("licenses_dependencies").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("legal_done").performClick()
        composeTestRule.onNodeWithTag("settings_about").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("legal_done").performClick()
    }

    @Test
    fun `home displays honest unavailable state when storage cannot be read`() {
        composeTestRule.setContent {
            ClenovaApp(
                storageRepository = FakeStorageRepository(StorageState.Unavailable),
                preferencesRepository = FakePreferencesRepository(),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Storage details unavailable").assertIsDisplayed()
    }
}
