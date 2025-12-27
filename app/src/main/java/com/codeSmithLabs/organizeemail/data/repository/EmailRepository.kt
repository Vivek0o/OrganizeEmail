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

class EmailRepository(private val authClient: GoogleAuthClient) {

    private val classifier = EmailClassifier()

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

    suspend fun getEmails(): List<EmailUI> {
        val account = authClient.getLastSignedInAccount() ?: throw Exception("User not signed in")
        val token = authClient.getAccessToken(account) ?: throw Exception("Failed to get access token")
        
        val service = RetrofitClient.getGmailService(token)
        
        try {
            // 1. Fetch List of IDs
            val listResponse = service.listMessages(maxResults = 100)
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
            attachments = attachments
        )
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
        
        part.parts?.let { parts ->
            // Prefer HTML
            val htmlPart = parts.find { it.mimeType == "text/html" }
            if (htmlPart?.body?.data != null) {
                return decodeBase64(htmlPart.body.data)
            }
            // Fallback to Plain Text
            val textPart = parts.find { it.mimeType == "text/plain" }
            if (textPart?.body?.data != null) {
                return decodeBase64(textPart.body.data)
            }
            
            // Recursively search
            for (subPart in parts) {
                val res = getBodyFromMessage(subPart)
                if (res.isNotEmpty()) return res
            }
        }
        
        return ""
    }

    private fun getAttachmentsFromMessage(part: MessagePart?, messageId: String): List<AttachmentUI> {
        val attachments = mutableListOf<AttachmentUI>()
        if (part == null) return attachments

        // Check if the current part is an attachment
        if (!part.filename.isNullOrEmpty() && part.body?.attachmentId != null) {
            attachments.add(
                AttachmentUI(
                    filename = part.filename,
                    mimeType = part.mimeType ?: "application/octet-stream",
                    size = part.body.size,
                    attachmentId = part.body.attachmentId,
                    partId = part.partId
                )
            )
        }

        // Recursively check sub-parts
        part.parts?.forEach { subPart ->
            attachments.addAll(getAttachmentsFromMessage(subPart, messageId))
        }

        return attachments
    }

    private fun decodeBase64(data: String): String {
        return try {
            val sanitized = data.replace("-", "+").replace("_", "/")
            val bytes = Base64.decode(sanitized, Base64.DEFAULT)
            String(bytes)
        } catch (e: Exception) {
            "Error decoding body"
        }
    }
}
