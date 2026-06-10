package com.example.whatsappstatussaver.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CUSTOM_SAVE_LOCATION = androidx.datastore.preferences.core.stringPreferencesKey("custom_save_location")
    }

    val customSaveLocationFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[CUSTOM_SAVE_LOCATION]
        }

    suspend fun setCustomSaveLocation(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(CUSTOM_SAVE_LOCATION)
            } else {
                preferences[CUSTOM_SAVE_LOCATION] = uri
            }
        }
    }

    val isDarkModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[IS_DARK_MODE] ?: false // Default false
        }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }
    
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true // Default true
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
