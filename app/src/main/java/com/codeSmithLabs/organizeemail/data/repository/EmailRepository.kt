package com.codeSmithLabs.organizeemail.data.repository

import android.util.Base64
import com.codeSmithLabs.organizeemail.data.auth.GoogleAuthClient
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import com.codeSmithLabs.organizeemail.data.model.GmailMessage
import com.codeSmithLabs.organizeemail.data.model.MessagePart
import com.codeSmithLabs.organizeemail.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import android.util.Log
import retrofit2.HttpException
import com.codeSmithLabs.organizeemail.data.model.GmailLabel
import com.codeSmithLabs.organizeemail.ml.EmailClassifier
import com.codeSmithLabs.organizeemail.data.model.AttachmentUI
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import java.io.FileOutputStream
import android.net.Uri
import com.codeSmithLabs.organizeemail.data.model.CleanupCategoryStats

class EmailRepository(
    private val authClient: GoogleAuthClient,
    private val context: Context
) {

    private val classifier = EmailClassifier()
    private val gson = Gson()
    
    suspend fun downloadAttachment(
        messageId: String,
        attachmentId: String,
        filename: String,
        mimeType: String
    ): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
                val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
                val apiService = RetrofitClient.getGmailService(token)
                
                val attachment = apiService.getAttachment(messageId, attachmentId)
                val dataBase64 = attachment.data ?: return@withContext null

                val sanitized = dataBase64.replace("-", "+").replace("_", "/")
                val bytes = Base64.decode(sanitized, Base64.DEFAULT)

                saveToDownloads(filename, bytes, mimeType)
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error downloading attachment", e)
                null
            }
        }
    }

    private fun saveToDownloads(filename: String, bytes: ByteArray, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(bytes)
                }
            }
            uri
        } else {
            try {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(bytes)
                }
                Uri.fromFile(file)
            } catch (e: Exception) {
                try {
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(bytes)
                    }
                    Uri.fromFile(file)
                } catch (e2: Exception) {
                    Log.e("EmailRepository", "Failed to save attachment", e2)
                    null
                }
            }
        }
    }

    suspend fun saveCleanupStats(stats: Map<String, Int>) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("cleanup_stats", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                stats.forEach { (key, value) ->
                    editor.putInt(key, value)
                }
                editor.apply()
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error saving cleanup stats", e)
            }
        }
    }

    suspend fun getCleanupStats(): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("cleanup_stats", Context.MODE_PRIVATE)
                mapOf(
                    "promotional" to prefs.getInt("promotional", 0),
                    "bank_ads" to prefs.getInt("bank_ads", 0),
                    "heavy" to prefs.getInt("heavy", 0)
                )
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error reading cleanup stats", e)
                emptyMap()
            }
        }
    }

    private fun getCacheFileName(labelId: String?): String {
        // Sanitize labelId to be a valid filename just in case
        val safeId = labelId?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "default"
        return "emails_cache_$safeId.json"
    }

    suspend fun getEmailsFromCache(labelId: String? = null): List<EmailUI> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, getCacheFileName(labelId))
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<List<EmailUI>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error reading cache", e)
                emptyList()
            }
        }
    }

    suspend fun saveEmailsToCache(emails: List<EmailUI>, labelId: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(emails)
                val file = File(context.filesDir, getCacheFileName(labelId))
                file.writeText(json)
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error saving cache", e)
            }
        }
    }

    suspend fun getLabelsFromCache(): List<GmailLabel> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "labels_cache.json")
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<List<GmailLabel>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error reading labels cache", e)
                emptyList()
            }
        }
    }

    suspend fun saveLabelsToCache(labels: List<GmailLabel>) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(labels)
                val file = File(context.filesDir, "labels_cache.json")
                file.writeText(json)
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error saving labels cache", e)
            }
        }
    }

    suspend fun getLabels(): List<GmailLabel> {
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")

        val service = RetrofitClient.getGmailService(token)

        return try {
            val response = service.listLabels()
            response.labels ?: emptyList()
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching labels", e)
            emptyList()
        }
    }

    suspend fun getEmails(labelId: String? = null): List<EmailUI> {
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
        
        val service = RetrofitClient.getGmailService(token)
        
        try {
            // 1. Fetch List of IDs
            val labelIds = if (labelId != null) listOf(labelId) else null
            val listResponse = service.listMessages(
                maxResults = 500,
                labelIds = labelIds
            )
            val messages = listResponse.messages ?: emptyList()

            // 2. Fetch Details for each ID (Parallel)
            return withContext(Dispatchers.IO) {
                messages.map { summary ->
                    async {
                        try {
                            val fullMessage = service.getMessage(summary.id)
                            mapToEmailUI(fullMessage)
                        } catch (e: Exception) {
                            Log.e("EmailRepository", "Error fetching individual message: ${summary.id}", e)
                            e.printStackTrace()
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = "API Error: ${e.code()} - $errorBody"
            Log.e("EmailRepository", errorMessage)
            throw Exception(errorMessage)
        }
    }

    private fun mapToEmailUI(message: GmailMessage): EmailUI {
        val headers = message.payload?.headers
        val sender = headers?.find { it.name.equals("From", ignoreCase = true) }?.value ?: "Unknown"
        val senderKey = deriveSenderKey(sender)
        val senderDomain = extractDomain(sender)
        val subject = headers?.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: "(No Subject)"
        val snippet = message.snippet ?: ""
        
        // Use Gmail labels to improve categorization
        val labelIds = message.labelIds ?: emptyList()
        
        val category = if (labelIds.contains("CATEGORY_PROMOTIONS")) {
             "Promotions"
        } else if (labelIds.contains("CATEGORY_SOCIAL") || labelIds.contains("CATEGORY_FORUMS")) {
             "Social"
        } else {
             // Fallback to local classifier
             classifier.classify(sender, subject, snippet)
        }
        
        val date = headers?.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""
        
        val body = getBodyFromMessage(message.payload)
        val attachments = getAttachmentsFromMessage(message.payload, message.id)
        
        val isUnread = labelIds.contains("UNREAD")
        val isImportant = labelIds.contains("IMPORTANT")
        val hasMeaningfulAttachment = attachments.any { isMeaningfulAttachment(it) }

        return EmailUI(
            id = message.id,
            sender = sender,
            senderKey = senderKey,
            senderDomain = senderDomain,
            category = category,
            subject = subject,
            date = date,
            snippet = snippet,
            body = body,
            attachments = attachments,
            isUnread = isUnread,
            hasMeaningfulAttachment = hasMeaningfulAttachment,
            labels = labelIds,
            isImportant = isImportant
        )
    }

    private fun isMeaningfulAttachment(attachment: AttachmentUI): Boolean {
        // 1. Size Check (> 50KB)
        if (attachment.size < 50 * 1024) return false

        // 2. Mime Type Check
        val meaningfulMimeTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // xlsx
            "application/zip",
            "application/x-zip-compressed"
        )
        
        return meaningfulMimeTypes.any { attachment.mimeType.startsWith(it, ignoreCase = true) } || 
               attachment.mimeType.startsWith("image/", ignoreCase = true)
    }

    private fun extractDomain(sender: String): String? {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})")
        val match = emailRegex.find(sender)
        return match?.groupValues?.getOrNull(1)
    }

    private fun deriveSenderKey(sender: String): String {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+)\\.[A-Za-z]{2,}")
        val match = emailRegex.find(sender)
        if (match != null && match.groupValues.size > 1) {
            val domain = match.groupValues[1]
            val parts = domain.split('.')
            val key = parts.firstOrNull { it.isNotEmpty() } ?: domain
            return key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        val nameRegex = Regex("^\\\"?([^<\\\"']+)\\\"?\\s*<")
        val nameMatch = nameRegex.find(sender)
        if (nameMatch != null && nameMatch.groupValues.size > 1) {
            val name = nameMatch.groupValues[1].trim()
            return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return sender.trim()
    }

    private fun getBodyFromMessage(part: MessagePart?): String {
        if (part == null) return ""
        
        if (part.body?.data != null) {
            return decodeBase64(part.body.data)
        }
        
        if (part.parts != null) {
            for (subPart in part.parts) {
                val body = getBodyFromMessage(subPart)
                if (body.isNotEmpty()) return body
            }
        }
        
        return ""
    }

    private fun decodeBase64(data: String): String {
        return try {
            val sanitized = data.replace("-", "+").replace("_", "/")
            val bytes = Base64.decode(sanitized, Base64.DEFAULT)
            String(bytes)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getAttachmentsFromMessage(part: MessagePart?, messageId: String): List<AttachmentUI> {
        val attachments = mutableListOf<AttachmentUI>()
        if (part == null) return attachments

        if (!part.filename.isNullOrEmpty() && part.body?.attachmentId != null) {
            attachments.add(
                AttachmentUI(
                    filename = part.filename,
                    mimeType = part.mimeType ?: "application/octet-stream",
                    size = part.body.size ?: 0,
                    attachmentId = part.body.attachmentId,
                    partId = part.partId
                )
            )
        }

        part.parts?.forEach { subPart ->
            attachments.addAll(getAttachmentsFromMessage(subPart, messageId))
        }

        return attachments
    }

    suspend fun trashEmail(id: String) {
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
        val service = RetrofitClient.getGmailService(token)
        service.trashMessage(id)
    }

    suspend fun trashEmails(ids: List<String>) {
        if (ids.isEmpty()) return
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
        val service = RetrofitClient.getGmailService(token)
        
        ids.chunked(50).forEach { chunk ->
            val request = com.codeSmithLabs.organizeemail.data.model.BatchModifyRequest(
                ids = chunk,
                addLabelIds = listOf("TRASH")
            )
            service.batchModify(request)
        }
    }

    suspend fun getCleanupEmails(type: String): List<EmailUI> {
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
        val service = RetrofitClient.getGmailService(token)

        val (labelIds, query) = when (type) {
            "promotional" -> Pair(listOf("CATEGORY_PROMOTIONS", "INBOX"), null)
            "heavy" -> Pair(null, "larger:5M")
            "bank_ads" -> Pair(listOf("CATEGORY_UPDATES"), null) 
            else -> Pair(null, null)
        }

        try {
            val listResponse = service.listMessages(maxResults = 100, labelIds = labelIds, query = query)
            val messages = listResponse.messages ?: emptyList()

            val emails = withContext(Dispatchers.IO) {
                messages.map { summary ->
                    async {
                        try {
                            val fullMessage = service.getMessage(summary.id)
                            mapToEmailUI(fullMessage)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            return if (type == "bank_ads") {
                 emails.filter { isBankAd(it) }
            } else {
                 emails
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching cleanup emails", e)
            return emptyList()
        }
    }

    private val adKeywords by lazy { loadAdKeywords() }

    private data class AdKeywords(
        @com.google.gson.annotations.SerializedName("ad_keywords") val adKeywords: List<String>
    )

    private fun loadAdKeywords(): List<String> {
        return try {
            val inputStream = context.assets.open("bank_ad_keywords.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, java.nio.charset.Charset.forName("UTF-8"))
            val data = gson.fromJson(json, AdKeywords::class.java)
            data.adKeywords.map { it.lowercase() }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error reading ad keywords", e)
            emptyList()
        }
    }
    
    suspend fun getCleanupStats(type: String): CleanupCategoryStats {
        val account = authClient.getLastSignedInAccount() ?: return CleanupCategoryStats(0, 0, 0)
        val token = authClient.getAccessToken(account) ?: return CleanupCategoryStats(0, 0, 0)
        val service = RetrofitClient.getGmailService(token)

        val (labelIds, query) = when (type) {
            "promotional" -> Pair(listOf("CATEGORY_PROMOTIONS", "INBOX"), null)
            "heavy" -> Pair(null, "larger:5M")
            "bank_ads" -> Pair(listOf("CATEGORY_UPDATES"), null)
            else -> Pair(null, null)
        }

        try {
            if (type == "bank_ads") {
                val listResponse = service.listMessages(maxResults = 100, labelIds = labelIds, query = query)
                val messages = listResponse.messages ?: emptyList()

                return withContext(Dispatchers.IO) {
                    // Load keywords once
                    val keywords = adKeywords
                    messages.map { summary ->
                        async {
                            try {
                                val msg = service.getMessage(summary.id, format = "metadata")
                                val headers = msg.payload?.headers
                                val subject = headers?.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
                                val sender = headers?.find { it.name.equals("From", ignoreCase = true) }?.value ?: ""
                                val snippet = msg.snippet ?: ""
                                
                                val bankKeywords = listOf("bank", "pay", "wallet", "card", "finance", "invest", "mutual", "fund", "stock", "insurance", "loan", "tax", "gst", "hdfc", "sbi", "axis", "icici", "kotak", "pnb", "bob", "paytm", "phonepe", "razorpay", "gpay", "googlepay", "cred", "zerodha", "groww", "upstox", "indmoney", "dhan", "navi")
                                
                                val content = (subject + " " + snippet).lowercase()
                                val senderLower = sender.lowercase()
                                
                                val isBankSender = bankKeywords.any { senderLower.contains(it) }
                                val hasAdContent = keywords.any { content.contains(it) }
                                
                                if (isBankSender && hasAdContent) {
                                    val size = msg.sizeEstimate ?: 0
                                    CleanupCategoryStats(1, size.toLong(), 0)
                                } else {
                                    CleanupCategoryStats(0, 0, 0)
                                }
                            } catch (e: Exception) {
                                CleanupCategoryStats(0, 0, 0)
                            }
                        }
                    }.awaitAll().fold(CleanupCategoryStats(0, 0, 0)) { acc, stats ->
                        CleanupCategoryStats(
                            acc.count + stats.count,
                            acc.sizeBytes + stats.sizeBytes,
                            acc.attachmentCount + stats.attachmentCount
                        )
                    }
                }
            } else if (type == "heavy") {
                val listResponse = service.listMessages(maxResults = 100, labelIds = labelIds, query = query)
                val messages = listResponse.messages ?: emptyList()
                
                return withContext(Dispatchers.IO) {
                    messages.map { summary ->
                        async {
                            try {
                                val msg = service.getMessage(summary.id, format = "minimal")
                                val size = msg.sizeEstimate ?: 0
                                CleanupCategoryStats(1, size.toLong(), 1) // Assume 1 attachment per heavy email
                            } catch (e: Exception) {
                                CleanupCategoryStats(0, 0, 0)
                            }
                        }
                    }.awaitAll().fold(CleanupCategoryStats(0, 0, 0)) { acc, stats ->
                        CleanupCategoryStats(
                            acc.count + stats.count,
                            acc.sizeBytes + stats.sizeBytes,
                            acc.attachmentCount + stats.attachmentCount
                        )
                    }
                }
            } else {
                val listResponse = service.listMessages(maxResults = 500, labelIds = labelIds, query = query)
                val fetchedCount = listResponse.messages?.size ?: 0
                val estimatedTotal = listResponse.resultSizeEstimate
                val count = if (fetchedCount < 500) fetchedCount else maxOf(fetchedCount, estimatedTotal)
                
                // Estimate size: 75KB per promotional email
                val estimatedSize = count * 75 * 1024L
                
                return CleanupCategoryStats(count, estimatedSize, 0)
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching cleanup stats", e)
            return CleanupCategoryStats(0, 0, 0)
        }
    }

    private fun isBankAd(email: EmailUI): Boolean {
        if (email.category != "Finance") return false
        val content = (email.subject + " " + email.snippet).lowercase()
        return adKeywords.any { content.contains(it) }
    }
}
