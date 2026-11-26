package com.clenova.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clenova.app.domain.model.AutopilotStatus
import com.clenova.app.data.agent.AgentScheduler
import com.clenova.app.data.agent.AgentReportStore
import com.clenova.app.domain.model.ThemePreference
import com.clenova.app.domain.model.UserPreferences
import com.clenova.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.clenovaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clenova_preferences",
)

class DataStorePreferencesRepository(
    private val context: Context,
) : PreferencesRepository {

    private object Keys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val STORAGE_GOAL = intPreferencesKey("storage_goal_gb")
        val CHECK_INTERVAL = intPreferencesKey("check_interval_days")
        val AGENT_CONFIGURED = booleanPreferencesKey("agent_configured")
        val AGENT_ENABLED = booleanPreferencesKey("agent_enabled")
    }

    override val userPreferences: Flow<UserPreferences> = context.clenovaDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val theme = prefs[Keys.THEME_PREFERENCE]
                ?.let { raw -> runCatching { ThemePreference.valueOf(raw) }.getOrNull() }
                ?: ThemePreference.LIGHT
            val hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true

            UserPreferences(
                themePreference = theme,
                hapticsEnabled = hapticsEnabled,
                autopilotStatus = when {
                    prefs[Keys.AGENT_CONFIGURED] != true -> AutopilotStatus.NotConfigured
                    prefs[Keys.AGENT_ENABLED] == true -> AutopilotStatus.Active
                    else -> AutopilotStatus.Paused
                },
                storageGoalGb = prefs[Keys.STORAGE_GOAL]?.takeIf { it in listOf(5, 10, 20, 30) } ?: 20,
                checkIntervalDays = prefs[Keys.CHECK_INTERVAL]?.takeIf { it in listOf(1, 3, 7) } ?: 1,
            )
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.clenovaDataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = preference.name
        }
    }

    override suspend fun setAutopilotPreferences(goalGb: Int, intervalDays: Int) {
        require(goalGb in listOf(5, 10, 20, 30))
        require(intervalDays in listOf(1, 3, 7))
        context.clenovaDataStore.edit { prefs ->
            prefs[Keys.STORAGE_GOAL] = goalGb
            prefs[Keys.CHECK_INTERVAL] = intervalDays
            prefs[Keys.AGENT_CONFIGURED] = true
            prefs[Keys.AGENT_ENABLED] = true
        }
        AgentReportStore(context).clear()
        AgentScheduler.activate(context, intervalDays)
    }

    override suspend fun setAutopilotEnabled(enabled: Boolean) {
        val updated = context.clenovaDataStore.edit { prefs ->
            require(prefs[Keys.AGENT_CONFIGURED] == true)
            prefs[Keys.AGENT_ENABLED] = enabled
        }
        if (enabled) AgentScheduler.activate(context, updated[Keys.CHECK_INTERVAL] ?: 1)
        else AgentScheduler.pause(context)
    }

    override suspend fun resetPreferences() {
        context.clenovaDataStore.edit { prefs ->
            prefs.remove(Keys.THEME_PREFERENCE)
            prefs.remove(Keys.HAPTICS_ENABLED)
            prefs.remove(Keys.STORAGE_GOAL)
            prefs.remove(Keys.CHECK_INTERVAL)
            prefs.remove(Keys.AGENT_CONFIGURED)
            prefs.remove(Keys.AGENT_ENABLED)
        }
        AgentScheduler.pause(context)
        AgentReportStore(context).clear()
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.clenovaDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }
}
