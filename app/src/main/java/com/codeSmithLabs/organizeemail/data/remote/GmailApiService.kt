package com.codeSmithLabs.organizeemail.data.remote

import com.codeSmithLabs.organizeemail.data.model.BatchModifyRequest
import com.codeSmithLabs.organizeemail.data.model.GmailLabelListResponse
import com.codeSmithLabs.organizeemail.data.model.GmailMessage
import com.codeSmithLabs.organizeemail.data.model.GmailMessageListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GmailApiService {
    @GET("gmail/v1/users/me/labels")
    suspend fun listLabels(): GmailLabelListResponse

    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Query("maxResults") maxResults: Int = 20,
        @Query("q") query: String? = null,
        @Query("labelIds") labelIds: List<String>? = null
    ): GmailMessageListResponse

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Path("id") id: String,
        @Query("format") format: String = "full"
    ): GmailMessage

    @GET("gmail/v1/users/me/messages/{messageId}/attachments/{id}")
    suspend fun getAttachment(
        @Path("messageId") messageId: String,
        @Path("id") id: String
    ): com.codeSmithLabs.organizeemail.data.model.MessageBody

    @retrofit2.http.POST("gmail/v1/users/me/messages/{id}/trash")
    suspend fun trashMessage(
        @Path("id") id: String
    ): GmailMessage

    @POST("gmail/v1/users/me/messages/batchModify")
    suspend fun batchModify(
        @Body request: BatchModifyRequest
    ): Unit
}
