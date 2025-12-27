package com.codeSmithLabs.organizeemail.ui.email

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeSmithLabs.organizeemail.data.model.EmailUI

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.viewinterop.AndroidView

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.codeSmithLabs.organizeemail.data.model.AttachmentUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    email: EmailUI?,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (email == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text("Email not found", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Header Section
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // From Line
                    Text(
                        text = "From: ${email.sender}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Date Line
                    Text(
                        text = formatFullDate(email.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Attachments Section
                    if (email.attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Attachments (${email.attachments.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            email.attachments.forEach { attachment ->
                                AttachmentCard(attachment)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                // WebView for HTML Content
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, email.body, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AttachmentCard(attachment: AttachmentUI) {
    ElevatedCard(
        onClick = { /* TODO: Implement download logic */ },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 200.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = attachment.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatFileSize(attachment.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatFileSize(size: Int): String {
    val kb = size / 1024.0
    return if (kb > 1024) {
        String.format(Locale.US, "%.1f MB", kb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}


// Helper to format date nicely (e.g. "Wed, 17 Dec 2025 07:07 AM")
fun formatFullDate(dateString: String): String {
    try {
        // Common Gmail date format: "Tue, 26 Dec 2024 10:30:00 +0000"
        // Sometimes it might vary, but this is the standard RFC 2822 format
        // We will try to parse it and convert to IST
        
        // Remove the timezone part if it exists (e.g. +0000 or (UTC)) to parse cleanly if needed, 
        // OR better: use a parser that handles it and then converts.
        
        // Simple heuristic parser for the user request
        val inputFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val date = inputFormat.parse(dateString)
        
        if (date != null) {
            val outputFormat = SimpleDateFormat("EEE, d MMM yyyy hh:mm a", Locale.ENGLISH)
            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata") // IST
            return outputFormat.format(date)
        }
    } catch (e: Exception) {
        // Fallback: try to just strip the +0000 part if parsing fails
        if (dateString.contains("+")) {
             return dateString.substringBefore("+").trim()
        }
    }
    return dateString
}
