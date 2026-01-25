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
            preferences[SYNC_ENABLED] ?: false 
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