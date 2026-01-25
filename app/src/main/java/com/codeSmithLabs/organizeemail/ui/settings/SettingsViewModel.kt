package com.codeSmithLabs.organizeemail.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeSmithLabs.organizeemail.data.settings.SettingsRepository
import com.codeSmithLabs.organizeemail.worker.SyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val syncManager = SyncManager(application)

    val syncFrequency: StateFlow<Int> = repository.syncFrequencyHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    val syncEnabled: StateFlow<Boolean> = repository.syncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateSyncFrequency(hours: Int) {
        viewModelScope.launch {
            repository.setSyncFrequency(hours)
            if (syncEnabled.value) {
                syncManager.scheduleSync(hours.toLong())
            }
        }
    }

    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncEnabled(enabled)
            if (enabled) {
                syncManager.scheduleSync(syncFrequency.value.toLong())
            } else {
                syncManager.cancelSync()
            }
        }
    }
}