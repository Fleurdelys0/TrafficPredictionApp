package com.example.trafficprediction.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// We define the DataStore instance as a context extension.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class ThemePreference {
    LIGHT, DARK, SYSTEM_DEFAULT
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    val themePreferenceFlow: Flow<ThemePreference> = context.dataStore.data
        .map { preferences ->
            ThemePreference.valueOf(
                preferences[PreferencesKeys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM_DEFAULT.name
            )
        }

    suspend fun updateThemePreference(themePreference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = themePreference.name
        }
    }
}
