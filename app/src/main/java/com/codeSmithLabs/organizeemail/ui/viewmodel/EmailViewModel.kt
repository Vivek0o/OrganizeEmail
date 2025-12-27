package com.codeSmithLabs.organizeemail.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeSmithLabs.organizeemail.data.auth.GoogleAuthClient
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import com.codeSmithLabs.organizeemail.data.repository.EmailRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.util.Log

import com.codeSmithLabs.organizeemail.data.model.GmailLabel

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EmailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authClient = GoogleAuthClient(application)
    private val repository = EmailRepository(authClient, application)

    // UI State
    private val _emails = MutableStateFlow<List<EmailUI>>(emptyList())
    val emails = _emails.asStateFlow()

    private val _labels = MutableStateFlow<List<GmailLabel>>(emptyList())
    val labels = _labels.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _user = MutableStateFlow<GoogleSignInAccount?>(null)
    val user = _user.asStateFlow()

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        _user.value = authClient.getLastSignedInAccount()
        if (_user.value != null) {
            // Load cache immediately if available
            viewModelScope.launch {
                val cachedEmails = repository.getEmailsFromCache()
                if (cachedEmails.isNotEmpty()) {
                    _emails.value = cachedEmails
                }
                
                val cachedLabels = repository.getLabelsFromCache()
                if (cachedLabels.isNotEmpty()) {
                    _labels.value = cachedLabels
                }

                // Trigger sync
                fetchEmails(isSync = true)
            }
        }
    }

    fun fetchEmails(isSync: Boolean = false, labelId: String? = null) {
        // Clear state immediately to avoid showing stale data from previous view
        if (!isSync) {
            _emails.value = emptyList()
            _loading.value = true
            _error.value = null
        }

        viewModelScope.launch {
            // 1. Try to load from cache immediately
            val cachedEmails = repository.getEmailsFromCache(labelId)
            
            // If we are just switching labels, we might want to load cached labels too?
            // Usually labels are global, so we don't need to re-fetch them for every label switch if we already have them.
            // But if we are in this function, we probably want to refresh them or at least ensure they are loaded.
            if (_labels.value.isEmpty()) {
                 val cachedLabels = repository.getLabelsFromCache()
                 if (cachedLabels.isNotEmpty()) {
                     _labels.value = cachedLabels
                 }
            }
            
            if (cachedEmails.isNotEmpty()) {
                _emails.value = cachedEmails
                // If we have cached data, we don't show the full loading screen, 
                // but we might want a "syncing" indicator (which we can add later if needed)
                _loading.value = false 
            } else {
                // If no cache, show loader and clear list
                _emails.value = emptyList()
                _loading.value = true
            }
            
            _error.value = null
            try {
                coroutineScope {
                    // 2. Fetch new data in parallel
                    val emailsDeferred = async { repository.getEmails(labelId) }
                    val labelsDeferred = async { repository.getLabels() }

                    val newEmails = emailsDeferred.await()
                    val newLabels = labelsDeferred.await()

                    // Update UI
                    _emails.value = newEmails
                    _labels.value = newLabels

                    // Update Cache
                    repository.saveLabelsToCache(newLabels)
                    repository.saveEmailsToCache(newEmails, labelId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("EmailViewModel", "Error fetching emails/labels", e)
                // Only show error if we have no data to show
                if (_emails.value.isEmpty()) {
                    _error.value = e.message ?: "Unknown error"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        _user.value = account
        if (account != null) {
            fetchEmails()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authClient.signOut()
            _user.value = null
            _emails.value = emptyList()
        }
    }
    
    fun getAuthClient(): GoogleAuthClient = authClient
}
