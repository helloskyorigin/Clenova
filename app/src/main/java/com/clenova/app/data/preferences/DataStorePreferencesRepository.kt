package com.clenova.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clenova.app.domain.model.AutopilotStatus
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
                autopilotStatus = AutopilotStatus.NotConfigured,
            )
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.clenovaDataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = preference.name
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.clenovaDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }
}
