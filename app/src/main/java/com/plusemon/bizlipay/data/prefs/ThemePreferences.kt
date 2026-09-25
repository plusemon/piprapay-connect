package com.plusemon.bizlipay.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Manages user theme preferences using Jetpack Preferences DataStore.
 * Persists system-wide Dark Mode vs Light Mode configuration across app restarts.
 */
class ThemePreferences private constructor(private val context: Context) {

    private val isDarkModeKey = booleanPreferencesKey(KEY_IS_DARK_MODE)

    /**
     * Flow emitting the current theme state.
     * Defaults to true (Dark Mode enabled) as default brand experience.
     */
    val isDarkModeFlow: Flow<Boolean> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[isDarkModeKey] ?: true
        }

    /**
     * Updates and persists the user's Dark Mode preference.
     */
    suspend fun setDarkMode(isDark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[isDarkModeKey] = isDark
        }
    }

    companion object {
        const val KEY_IS_DARK_MODE = "key_is_dark_mode"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
