package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.twinThemeStore: DataStore<Preferences> by preferencesDataStore(
    name = "twin_theme_prefs"
)

class ThemePreferences(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("twin_dark_mode")

    val darkModeFlow: Flow<Boolean> = context.twinThemeStore.data.map { prefs ->
        prefs[darkModeKey] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.twinThemeStore.edit { it[darkModeKey] = enabled }
    }
}
