package com.codeSmithLabs.organizeemail.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.booleanPreferencesKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val SYNC_FREQUENCY_HOURS = intPreferencesKey("sync_frequency_hours")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }

    val syncFrequencyHours: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SYNC_FREQUENCY_HOURS] ?: 12 // Default to 12 hours
        }

    val syncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SYNC_ENABLED] ?: false // Default to false (opt-in) or true? User asked for feature, maybe default true? Let's stick to false for safety or true for convenience. 
            // The user said "if enable then only background sync should happen". Usually sync is on by default in apps.
            // But let's default to TRUE so it works out of box, unless user turns it off.
            preferences[SYNC_ENABLED] ?: true 
        }

    suspend fun setSyncFrequency(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_FREQUENCY_HOURS] = hours
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_ENABLED] = enabled
        }
    }
}