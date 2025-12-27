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
                // Trigger sync
                fetchEmails(isSync = true)
            }
        }
    }

    fun fetchEmails(isSync: Boolean = false) {
        viewModelScope.launch {
            // Only show full loading if we have no data
            if (_emails.value.isEmpty()) {
                _loading.value = true
            }
            
            _error.value = null
            try {
                val newEmails = repository.getEmails()
                _emails.value = newEmails
                _labels.value = repository.getLabels()
                
                // Save to cache
                repository.saveEmailsToCache(newEmails)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("EmailViewModel", "Error fetching emails", e)
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
