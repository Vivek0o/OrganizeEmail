package com.codeSmithLabs.organizeemail.data.model

import com.google.gson.annotations.SerializedName

data class GmailMessageListResponse(
    @SerializedName("messages") val messages: List<MessageSummary>?,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("resultSizeEstimate") val resultSizeEstimate: Int
)

data class MessageSummary(
    @SerializedName("id") val id: String,
    @SerializedName("threadId") val threadId: String
)

data class GmailMessage(
    @SerializedName("id") val id: String,
    @SerializedName("threadId") val threadId: String,
    @SerializedName("labelIds") val labelIds: List<String>?,
    @SerializedName("snippet") val snippet: String?,
    @SerializedName("payload") val payload: MessagePart?,
    @SerializedName("sizeEstimate") val sizeEstimate: Int?,
    @SerializedName("internalDate") val internalDate: Long
)

data class MessagePart(
    @SerializedName("partId") val partId: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("filename") val filename: String?,
    @SerializedName("headers") val headers: List<MessageHeader>?,
    @SerializedName("body") val body: MessageBody?,
    @SerializedName("parts") val parts: List<MessagePart>?
)

data class MessageHeader(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: String
)

data class MessageBody(
    @SerializedName("attachmentId") val attachmentId: String?,
    @SerializedName("data") val data: String?,
    @SerializedName("size") val size: Int
)

data class EmailUI(
    val id: String,
    val sender: String,
    val senderKey: String,
    val senderDomain: String?,
    val category: String, // Finance, Jobs, Social, Travel, Promotions, Updates, Forums, Other
    val subject: String,
    val date: String,
    val snippet: String,
    val body: String,
    val attachments: List<AttachmentUI> = emptyList(),
    val isUnread: Boolean = false,
    val hasMeaningfulAttachment: Boolean = false
)

data class AttachmentUI(
    val filename: String,
    val mimeType: String,
    val size: Int,
    val attachmentId: String?,
    val partId: String?
)

data class GmailLabelListResponse(
    @SerializedName("labels") val labels: List<GmailLabel>?
)

data class GmailLabel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String
)

data class BatchModifyRequest(
    @SerializedName("ids") val ids: List<String>,
    @SerializedName("addLabelIds") val addLabelIds: List<String> = emptyList(),
    @SerializedName("removeLabelIds") val removeLabelIds: List<String> = emptyList()
)

data class CleanupCategoryStats(
    val count: Int,
    val sizeBytes: Long,
    val attachmentCount: Int
)
