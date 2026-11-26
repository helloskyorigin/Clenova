package com.clenova.app.domain.repository

import com.clenova.app.domain.model.ThemePreference
import com.clenova.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setThemePreference(preference: ThemePreference)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setAutopilotPreferences(goalGb: Int, intervalDays: Int)
}
