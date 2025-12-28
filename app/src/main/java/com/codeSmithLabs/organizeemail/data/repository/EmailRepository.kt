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
            val listResponse = service.listMessages(maxResults = 100, labelIds = labelIds)
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
        
        // Use the classifier model
        val category = classifier.classify(sender, subject, snippet)
        
        val date = headers?.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""
        
        val body = getBodyFromMessage(message.payload)
        val attachments = getAttachmentsFromMessage(message.payload, message.id)
        
        val isUnread = message.labelIds?.contains("UNREAD") == true
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
            hasMeaningfulAttachment = hasMeaningfulAttachment
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

    suspend fun getCleanupCount(type: String): Int {
        val account = authClient.getLastSignedInAccount() ?: return 0
        val token = authClient.getAccessToken(account) ?: return 0
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
                    messages.map { summary ->
                        async {
                            try {
                                val msg = service.getMessage(summary.id, format = "metadata")
                                val headers = msg.payload?.headers
                                val subject = headers?.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
                                val sender = headers?.find { it.name.equals("From", ignoreCase = true) }?.value ?: ""
                                val snippet = msg.snippet ?: ""
                                
                                val bankKeywords = listOf("bank", "pay", "wallet", "card", "finance", "invest", "mutual", "fund", "stock", "insurance", "loan", "tax", "gst", "hdfc", "sbi", "axis", "icici", "kotak", "pnb", "bob", "paytm", "phonepe", "razorpay", "gpay", "googlepay", "cred", "zerodha", "groww", "upstox", "indmoney", "dhan", "navi")
                                val transactionalKeywords = listOf("statement", "bill", "invoice", "transaction", "alert", "otp", "debit", "credit", "payment", "received", "sent")
                                
                                val content = (subject + " " + snippet).lowercase()
                                val senderLower = sender.lowercase()
                                
                                val isBankSender = bankKeywords.any { senderLower.contains(it) }
                                val isTransactional = transactionalKeywords.any { content.contains(it) }
                                
                                isBankSender && !isTransactional
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }.awaitAll().count { it }
                }
            } else {
                val listResponse = service.listMessages(maxResults = 500, labelIds = labelIds, query = query)
                return listResponse.messages?.size ?: 0
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching cleanup count", e)
            return 0
        }
    }

    private fun isBankAd(email: EmailUI): Boolean {
        if (email.category != "Finance") return false
        val transactionalKeywords = listOf("statement", "bill", "invoice", "transaction", "alert", "otp", "debit", "credit", "payment", "received", "sent")
        val content = (email.subject + " " + email.snippet).lowercase()
        return !transactionalKeywords.any { content.contains(it) }
    }
}
