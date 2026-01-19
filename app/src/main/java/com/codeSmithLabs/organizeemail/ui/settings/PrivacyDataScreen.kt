package com.codeSmithLabs.organizeemail.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codeSmithLabs.organizeemail.R
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueEnd
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    // val uriHandler = LocalUriHandler.current // Not needed anymore

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
                    title = { Text("Privacy & Data") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_security),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "We value your privacy. Your data stays yours.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
            }

            PrivacySection(
                title = "What data we access",
                icon = Icons.Default.Info,
                items = listOf(
                    "Gmail metadata (sender, subject, date, snippet)"
                )
            )

            PrivacySection(
                title = "Why we access it",
                icon = Icons.Default.Check,
                items = listOf(
                    "Organizing emails into categories",
                    "Cleaning up promotional and heavy emails"
                )
            )

            PrivacySection(
                title = "What we do NOT do",
                icon = Icons.Default.Close,
                items = listOf(
                    "No ads",
                    "No selling data",
                    "No external sharing"
                ),
                isNegative = true
            )

            Button(
                onClick = onPrivacyPolicyClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =  Color(0xFF000000)
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Read Detailed Privacy Policy")
            }
        }
    }
}

@Composable
fun PrivacySection(
    title: String,
    icon: ImageVector,
    items: List<String>,
    isNegative: Boolean = false
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isNegative) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
