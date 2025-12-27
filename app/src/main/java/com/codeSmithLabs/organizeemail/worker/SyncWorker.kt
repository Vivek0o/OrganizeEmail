package com.codeSmithLabs.organizeemail.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codeSmithLabs.organizeemail.data.auth.GoogleAuthClient
import com.codeSmithLabs.organizeemail.data.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val authClient = GoogleAuthClient(applicationContext)
                val repository = EmailRepository(authClient, applicationContext)
                
                // Check if user is logged in
                if (authClient.getLastSignedInAccount() == null) {
                    return@withContext Result.failure()
                }

                // Fetch latest emails (INBOX)
                val newEmails = repository.getEmails(labelId = null) 
                repository.saveEmailsToCache(newEmails, labelId = null)
                
                // Fetch labels
                val labels = repository.getLabels()
                repository.saveLabelsToCache(labels)

                // Check for unread emails to notify
                val unreadCount = newEmails.count { it.isUnread }
                if (unreadCount > 0) {
                    sendNotification(unreadCount)
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    private fun sendNotification(unreadCount: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "email_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Email Sync", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Use a system icon or available resource. 
        // Assuming R.drawable.ic_launcher_foreground exists (standard), if not, fallback to system.
        // Since I can't guarantee R.drawable exists without checking, I'll use android.R.drawable.ic_dialog_email
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email) 
            .setContentTitle("New Emails Synced")
            .setContentText("You have $unreadCount unread emails.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}