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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers

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

    private var fetchJob: Job? = null
    private val emailsCache = mutableMapOf<String, List<EmailUI>>()

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
        val key = labelId ?: "INBOX"

        // Cancel previous fetch to avoid race conditions (e.g., background sync overwriting label fetch)
        fetchJob?.cancel()

        // 1. Check Memory Cache first (Fastest)
        val memoryData = emailsCache[key]
        if (memoryData != null && memoryData.isNotEmpty()) {
            _emails.value = memoryData
            _loading.value = false
        } else {
             // If not in memory, show loader or clear if needed
             if (!isSync) {
                 _emails.value = emptyList()
                 _loading.value = true
             }
             _error.value = null
        }

        fetchJob = viewModelScope.launch {
            // 2. Try Disk Cache if Memory missed
            if (memoryData == null) {
                val cachedEmails = repository.getEmailsFromCache(labelId)
                
                // Ensure labels are loaded
                if (_labels.value.isEmpty()) {
                     val cachedLabels = repository.getLabelsFromCache()
                     if (cachedLabels.isNotEmpty()) {
                         _labels.value = cachedLabels
                     }
                }
                
                if (cachedEmails.isNotEmpty()) {
                    _emails.value = cachedEmails
                    emailsCache[key] = cachedEmails // Populate memory
                    _loading.value = false 
                } else {
                    // Only show loader if we really have nothing
                    if (_emails.value.isEmpty()) {
                        _loading.value = true
                    }
                }
            }
            
            _error.value = null
            try {
                coroutineScope {
                    // 3. Fetch new data in parallel
                    val emailsDeferred = async { repository.getEmails(labelId) }
                    val labelsDeferred = async { repository.getLabels() }

                    val newEmails = emailsDeferred.await()
                    val newLabels = labelsDeferred.await()

                    // Update UI
                    _emails.value = newEmails
                    _labels.value = newLabels

                    // Update Cache
                    emailsCache[key] = newEmails
                    repository.saveLabelsToCache(newLabels)
                    repository.saveEmailsToCache(newEmails, labelId)

                    // Trigger Pre-fetch if this was the initial load (no labelId or Inbox)
                    if (labelId == null) {
                        prefetchUserLabels(newLabels)
                    }
                }
                _loading.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("EmailViewModel", "Error fetching emails/labels", e)
                // Only show error if we have no data to show
                if (_emails.value.isEmpty()) {
                    _error.value = e.message ?: "Unknown error"
                }
                _loading.value = false
            }
        }
    }

    private fun prefetchUserLabels(labels: List<GmailLabel>) {
        viewModelScope.launch(Dispatchers.IO) {
            labels.filter { it.type == "user" }.forEach { label ->
                try {
                    val emails = repository.getEmails(label.id)
                    repository.saveEmailsToCache(emails, label.id)
                } catch (e: Exception) {
                    Log.e("EmailViewModel", "Prefetch failed for ${label.name}", e)
                }
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
            emailsCache.clear()
        }
    }
    
    fun getAuthClient(): GoogleAuthClient = authClient
}
