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
import kotlinx.coroutines.awaitAll

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

import com.codeSmithLabs.organizeemail.data.model.CleanupCategoryStats

class EmailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authClient = GoogleAuthClient(application)
    private val repository = EmailRepository(authClient, application)

    // UI State
    private val _emails = MutableStateFlow<List<EmailUI>>(emptyList())
    val emails = _emails.asStateFlow()

    // Cleanup Assistant Stats
    private val _promotionalCount = MutableStateFlow(0)
    val promotionalCount = _promotionalCount.asStateFlow()

    private val _bankAdCount = MutableStateFlow(0)
    val bankAdCount = _bankAdCount.asStateFlow()

    private val _heavyEmailCount = MutableStateFlow(0)
    val heavyEmailCount = _heavyEmailCount.asStateFlow()
    
    private val _cleanupStats = MutableStateFlow(CleanupCategoryStats(0, 0, 0))
    val cleanupStats = _cleanupStats.asStateFlow()
    
    fun refreshCleanupStats() {
        viewModelScope.launch {
             try {
                 val promoDeferred = async { repository.getCleanupStats("promotional") }
                 val bankDeferred = async { repository.getCleanupStats("bank_ads") }
                 val heavyDeferred = async { repository.getCleanupStats("heavy") }

                 val pStats = promoDeferred.await()
                 val bStats = bankDeferred.await()
                 val hStats = heavyDeferred.await()

                 _promotionalCount.value = pStats.count
                 _bankAdCount.value = bStats.count
                 _heavyEmailCount.value = hStats.count
                 
                 val totalCount = pStats.count + bStats.count + hStats.count
                 val totalSize = pStats.sizeBytes + bStats.sizeBytes + hStats.sizeBytes
                 val totalAttachments = pStats.attachmentCount + bStats.attachmentCount + hStats.attachmentCount
                 
                 _cleanupStats.value = CleanupCategoryStats(totalCount, totalSize, totalAttachments)
                 
                 // Cache the new stats (counts only for now)
                 repository.saveCleanupStats(mapOf(
                     "promotional" to pStats.count,
                     "bank_ads" to bStats.count,
                     "heavy" to hStats.count
                 ))
             } catch (e: Exception) {
                 Log.e("EmailViewModel", "Error refreshing cleanup stats", e)
             }
        }
    }

    fun fetchCleanupEmails(type: String) {
        fetchJob?.cancel()
        
        _error.value = null
        
        fetchJob = viewModelScope.launch {
            // 1. Try Cache
            val cached = repository.getEmailsFromCache(type)
            if (cached.isNotEmpty()) {
                _emails.value = cached
            } else {
                _loading.value = true
                _emails.value = emptyList()
            }
            
            // 2. Fetch Network
            try {
                val emails = repository.getCleanupEmails(type)
                _emails.value = emails
                repository.saveEmailsToCache(emails, type)
            } catch (e: Exception) {
                 if (_emails.value.isEmpty()) {
                     _error.value = "Failed to load emails: ${e.message}"
                 }
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun deleteEmail(emailId: String) {
        deleteEmails(listOf(emailId))
    }

    fun deleteEmails(emailIds: List<String>) {
        viewModelScope.launch {
            try {
                // Optimistic UI update
                val previousEmails = _emails.value
                _emails.value = _emails.value.filter { !emailIds.contains(it.id) }
                
                // Batch API call
                try {
                    repository.trashEmails(emailIds)
                    // Update stats
                    refreshCleanupStats()
                } catch (e: Exception) {
                    Log.e("EmailViewModel", "Failed to trash emails", e)
                    // Revert UI on failure if needed, or just let next sync handle it
                    // For now, let's just log. 
                    // Ideally we should show a snackbar.
                }
            } catch (e: Exception) {
                Log.e("EmailViewModel", "Error deleting emails", e)
            }
        }
    }

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
                
                // Load cached cleanup stats
                val cachedStats = repository.getCleanupStats()
                if (cachedStats.isNotEmpty()) {
                    _promotionalCount.value = cachedStats["promotional"] ?: 0
                    _bankAdCount.value = cachedStats["bank_ads"] ?: 0
                    _heavyEmailCount.value = cachedStats["heavy"] ?: 0
                }

                // Trigger sync
                fetchEmails(isSync = true)
                refreshCleanupStats()
                
                // Pre-fetch cleanup categories in background
                prefetchCleanupCategories()
            }
        }
    }

    private fun prefetchCleanupCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            listOf("promotional", "bank_ads", "heavy").forEach { type ->
                try {
                    val emails = repository.getCleanupEmails(type)
                    if (emails.isNotEmpty()) {
                        repository.saveEmailsToCache(emails, type)
                    }
                } catch (e: Exception) {
                    Log.e("EmailViewModel", "Prefetch failed for $type", e)
                }
            }
        }
    }

    fun fetchEmails(isSync: Boolean = false, labelId: String? = null) {
        val key = labelId ?: "ALL_MAIL"

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

    fun downloadAttachment(messageId: String, attachment: com.codeSmithLabs.organizeemail.data.model.AttachmentUI, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (attachment.attachmentId == null) {
                onResult(false)
                return@launch
            }
            val uri = repository.downloadAttachment(messageId, attachment.attachmentId, attachment.filename, attachment.mimeType)
            onResult(uri != null)
        }
    }
}
