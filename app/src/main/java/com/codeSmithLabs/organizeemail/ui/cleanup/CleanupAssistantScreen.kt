package com.codeSmithLabs.organizeemail.ui.cleanup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CheckCircle
import com.codeSmithLabs.organizeemail.data.model.CleanupCategoryStats
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueEnd
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupAssistantScreen(
    promotionalCount: Int,
    bankAdCount: Int,
    heavyEmailCount: Int,
    cleanupStats: CleanupCategoryStats,
    onBackClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var showCleanupComplete by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GradientBlueStart.copy(alpha = 0.5f),
                                GradientBlueEnd.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = { Text("Cleanup Assistant") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Black,
                        actionIconContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showCleanupComplete) {
                CleanupCompleteCard(
                    freedSpace = formatFileSize(cleanupStats.sizeBytes)
                )
            } else {
                CleanupPreviewCard(
                    stats = cleanupStats,
                    onCleanupClick = { 
                        showCleanupComplete = true
                    }
                )
            }

            CleanupCard(
                title = "Promotional Emails",
                description = "Newsletters, offers, and shopping updates.",
                count = promotionalCount,
                icon = Icons.Default.ShoppingCart,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onCategoryClick("promotional") }
            )

            CleanupCard(
                title = "Bank Advertisements",
                description = "Informational emails and loan offers (excluding statements).",
                count = bankAdCount,
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onCategoryClick("bank_ads") }
            )

            CleanupCard(
                title = "Heavy Emails",
                description = "Emails with large attachments (> 5MB).",
                count = heavyEmailCount,
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error,
                onClick = { onCategoryClick("heavy") }
            )
        }
    }
}

@Composable
fun CleanupPreviewCard(
    stats: CleanupCategoryStats,
    onCleanupClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            GradientBlueStart.copy(alpha = 0.1f),
                            GradientBlueEnd.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cleanup Preview",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatRow("Emails to delete", String.format("%,d", stats.count), Color.Black)
            StatRow("Attachments found", String.format("%,d", stats.attachmentCount), Color.Black)
            StatRow("Estimated space to free", "~" + formatFileSize(stats.sizeBytes), Color.Black)
            
            /* 
            // Button hidden for now as we don't have bulk delete logic yet
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCleanupClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clean Up All")
            }
            */
        }
    }
}

@Composable
fun CleanupCompleteCard(
    freedSpace: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFFE8F5E9) // Light Green
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF2E7D32), // Dark Green
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Cleanup Complete",
                style = MaterialTheme.typography.titleLarge,
                color = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "You freed $freedSpace of email storage",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.9f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024 * 1024.0)
    return if (mb >= 1000) {
        String.format("%.1f GB", mb / 1024)
    } else {
        String.format("%.0f MB", mb)
    }
}

@Composable
fun CleanupCard(
    title: String,
    description: String,
    count: Int,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Badge(
                containerColor = color.copy(alpha = 0.1f),
                contentColor = color
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
