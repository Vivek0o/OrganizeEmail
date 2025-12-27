package com.codeSmithLabs.organizeemail.ui.email

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import com.codeSmithLabs.organizeemail.data.model.GmailLabel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailListScreen(
    emails: List<EmailUI>,
    labels: List<GmailLabel> = emptyList(),
    user: GoogleSignInAccount? = null,
    isLoading: Boolean,
    error: String?,
    onEmailClick: (EmailUI) -> Unit,
    onSignOutClick: () -> Unit,
    title: String = "OrganizeEmail",
    onSenderClick: ((String) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    showCategories: Boolean = false,
    showAllEmails: Boolean = true,
    onBackClick: (() -> Unit)? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Profile Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                HorizontalDivider()

                // Labels List
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = "Labels",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(labels.filter { it.type == "user" }) { label ->
                        NavigationDrawerItem(
                            label = { Text(text = label.name) },
                            selected = false,
                            onClick = { /* Handle label click later */ },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                HorizontalDivider()

                // Logout Button
                NavigationDrawerItem(
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = onSignOutClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (showCategories) "Top Categories" else title) },
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else if (onCategoryClick != null || onSenderClick != null && showCategories) { // Show Menu on main/category screen
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Removed direct logout from top bar, moved to drawer
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    LoadingSyncView()
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    if (showCategories && onCategoryClick != null) {
                        CategoryCardGrid(emails = emails, onCategoryClick = onCategoryClick)
                    } else if (onSenderClick != null && !showAllEmails) {
                        SenderCardGrid(emails = emails, onSenderClick = onSenderClick)
                    } else {
                        // Level 3: Email List (for specific sender)
                        LazyColumn {
                            items(emails) { email ->
                                EmailItem(email, onClick = { onEmailClick(email) })
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCardGrid(emails: List<EmailUI>, onCategoryClick: (String) -> Unit) {
    val groups = emails.groupBy { it.category }.toList().sortedByDescending { it.second.size }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(groups) { (category, list) ->
            CategoryCard(category = category, emails = list, onClick = { onCategoryClick(category) })
        }
    }
}

@Composable
fun CategoryCard(category: String, emails: List<EmailUI>, onClick: () -> Unit) {
    val color = getCategoryColor(category)
    val senders = remember(emails) {
        emails.distinctBy { it.senderKey }.take(4)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIconGrid(senders = senders)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${emails.size} Emails",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryIconGrid(senders: List<EmailUI>) {
    val count = senders.size
    val spacing = 2.dp
    
    when (count) {
        1 -> {
             val sender = senders[0]
             SenderAvatar(
                 name = sender.senderKey,
                 domain = sender.senderDomain,
                 modifier = Modifier.size(40.dp),
                 textStyle = MaterialTheme.typography.titleLarge
             )
        }
        2 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                senders.forEach { sender ->
                    SenderAvatar(
                        name = sender.senderKey,
                        domain = sender.senderDomain,
                        modifier = Modifier.size(28.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        3 -> {
             Column(
                 verticalArrangement = Arrangement.spacedBy(spacing), 
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                     senders.take(2).forEach { sender ->
                         SenderAvatar(
                            name = sender.senderKey,
                            domain = sender.senderDomain,
                            modifier = Modifier.size(24.dp),
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                     }
                 }
                 SenderAvatar(
                    name = senders[2].senderKey,
                    domain = senders[2].senderDomain,
                    modifier = Modifier.size(24.dp),
                    textStyle = MaterialTheme.typography.labelSmall
                )
             }
        }
        4 -> {
             Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                 Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                     senders.take(2).forEach { sender ->
                         SenderAvatar(
                            name = sender.senderKey,
                            domain = sender.senderDomain,
                            modifier = Modifier.size(24.dp),
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                     }
                 }
                 Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                     senders.drop(2).take(2).forEach { sender ->
                         SenderAvatar(
                            name = sender.senderKey,
                            domain = sender.senderDomain,
                            modifier = Modifier.size(24.dp),
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                     }
                 }
             }
        }
    }
}

@Composable
fun SenderAvatar(
    name: String,
    domain: String?,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val faviconUrl = if (domain != null) "https://www.google.com/s2/favicons?domain=$domain&sz=64" else null
    val randomColor = remember(name) { getColorForName(name) }
    var imageLoadFailed by remember { mutableStateOf(false) }

    if (faviconUrl != null && !imageLoadFailed) {
        AsyncImage(
            model = faviconUrl,
            contentDescription = name,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            onError = { imageLoadFailed = true }
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(randomColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = textStyle,
                color = Color.White
            )
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Finance" -> Color(0xFF4CAF50) // Green
        "Jobs" -> Color(0xFF2196F3) // Blue
        "Shopping" -> Color(0xFFFF9800) // Orange
        "Travel" -> Color(0xFF00BCD4) // Cyan
        "Social" -> Color(0xFFE91E63) // Pink
        "Tech" -> Color(0xFF607D8B) // Blue Grey
        "Entertainment" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF757575) // Grey
    }
}

@Composable
fun SenderCardGrid(emails: List<EmailUI>, onSenderClick: (String) -> Unit) {
    val groups = emails.groupBy { it.senderKey }.toList().sortedByDescending { it.second.size }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(groups) { (key, list) ->
            val domain = list.firstNotNullOfOrNull { it.senderDomain }
            SenderCard(key, domain, list.size, onClick = { onSenderClick(key) })
        }
    }
}

@Composable
fun SenderCard(name: String, domain: String?, count: Int, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val faviconUrl = if (domain != null) "https://www.google.com/s2/favicons?domain=$domain&sz=64" else null
            val randomColor = remember(name) { getColorForName(name) }
            
            // State to track if image failed to load
            var imageLoadFailed by remember { mutableStateOf(false) }

            if (faviconUrl != null && !imageLoadFailed) {
                AsyncImage(
                    model = faviconUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { imageLoadFailed = true }
                )
            } else {
                // Fallback Icon (Show if no URL OR if URL failed to load)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(randomColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmailItem(email: EmailUI, onClick: () -> Unit) {
    val senderName = email.sender.substringBefore("<").trim()
    val randomColor = remember(email.senderKey) { getColorForName(email.senderKey) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Sender Avatar (Favicon or Initials)
        if (email.senderDomain != null) {
            AsyncImage(
                model = "https://www.google.com/s2/favicons?domain=${email.senderDomain}&sz=64",
                contentDescription = senderName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.AccountCircle) // Fallback handled by AsyncImage? No, explicit fallback needed usually but AsyncImage handles nulls
            )
        } else {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(randomColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = senderName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Email Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatEmailDate(email.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = email.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper to format date like standard mail apps (e.g., "10:30 AM" or "Dec 25")
fun formatEmailDate(dateString: String): String {
    // Basic cleaning, in a real app use SimpleDateFormat or java.time
    // Gmail API date format: "Tue, 26 Dec 2024 10:30:00 +0000"
    return try {
        val parts = dateString.split(" ")
        if (parts.size >= 4) {
             // Return "Day Month" e.g., "26 Dec"
             "${parts[1]} ${parts[2]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}


fun getColorForName(name: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFEC407A), // Pink
        Color(0xFFAB47BC), // Purple
        Color(0xFF7E57C2), // Deep Purple
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF42A5F5), // Blue
        Color(0xFF29B6F6), // Light Blue
        Color(0xFF26C6DA), // Cyan
        Color(0xFF26A69A), // Teal
        Color(0xFF66BB6A), // Green
        Color(0xFF9CCC65), // Light Green
        Color(0xFFD4E157), // Lime
        Color(0xFFFFCA28), // Amber
        Color(0xFFFFA726), // Orange
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF8D6E63), // Brown
        Color(0xFF78909C)  // Blue Grey
    )
    val hash = name.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}

@Composable
fun LoadingSyncView() {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Syncing",
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { this.alpha = alpha },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Syncing Emails...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Organizing your inbox",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LinearProgressIndicator(
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
