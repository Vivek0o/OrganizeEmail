package com.codeSmithLabs.organizeemail.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeSmithLabs.organizeemail.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueEnd
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val frequency by viewModel.syncFrequency.collectAsState()
    val isSyncEnabled by viewModel.syncEnabled.collectAsState()

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
                    title = { Text("Settings") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Sync Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Master Toggle Card
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Sync",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automatically fetch emails in the background",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSyncEnabled,
                            onCheckedChange = { viewModel.toggleSync(it) }
                        )
                    }

                    // Frequency Options (Nested)
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .alpha(if (isSyncEnabled) 1f else 0.5f)
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                        
                        var expanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isSyncEnabled) { expanded = !expanded }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Column {
                                Text(
                                    text = "Sync Frequency",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                if (!expanded) {
                                    Text(
                                        text = "Every $frequency hours",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                             }
                             
                             Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = Color.Black
                             )
                        }

                        if (expanded) {
                            val options = listOf(8, 12, 24)
                            options.forEach { hours ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isSyncEnabled) { 
                                            viewModel.updateSyncFrequency(hours) 
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (frequency == hours),
                                        onClick = { viewModel.updateSyncFrequency(hours) },
                                        enabled = isSyncEnabled,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = Color.Black.copy(alpha = 0.6f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Every $hours hours",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Privacy Card
            ElevatedCard(
                onClick = onPrivacyClick,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_privacy),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Privacy & Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "See how we handle your data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}