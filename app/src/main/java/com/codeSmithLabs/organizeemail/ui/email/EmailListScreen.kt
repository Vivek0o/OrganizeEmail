package com.codeSmithLabs.organizeemail.ui.email

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codeSmithLabs.organizeemail.R
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import com.codeSmithLabs.organizeemail.data.model.GmailLabel
import com.codeSmithLabs.organizeemail.ui.common.AppIcon
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueEnd
import com.codeSmithLabs.organizeemail.ui.theme.GradientBlueStart
import com.codeSmithLabs.organizeemail.ui.theme.GradientPinkEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailListScreen(
    emails: List<EmailUI>,
    labels: List<GmailLabel> = emptyList(),
    user: GoogleSignInAccount? = null,
    isLoading: Boolean,
    syncProgress: Float = 0f,
    error: String?,
    onEmailClick: (EmailUI) -> Unit,
    onSignOutClick: () -> Unit,
    title: String = "OrganizeEmail",
    onSenderClick: ((String) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    showCategories: Boolean = false,
    showAllEmails: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    onLabelClick: ((String) -> Unit)? = null,
    onSmartFilterClick: ((String) -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onCleanupClick: (() -> Unit)? = null,
    onDeleteEmails: ((List<String>) -> Unit)? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Selection Mode State
    var selectedEmailIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedEmailIds.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.White,
                windowInsets = WindowInsets(0.dp) // Extend drawer to top edge
            ) {
                // Profile Section
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
                        .padding(24.dp)
                ) {
                    Column {
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
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                    }
                }

                HorizontalDivider()

                // Labels List
                var isLabelsExpanded by remember { mutableStateOf(false) }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        NavigationDrawerItem(
                            label = { Text("Labels") },
                            selected = false,
                            onClick = { isLabelsExpanded = !isLabelsExpanded },
                            icon = { Icon(painter = painterResource(R.drawable.ic_label), contentDescription = null) },
                            badge = {
                                Icon(
                                    imageVector = if (isLabelsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isLabelsExpanded) "Collapse" else "Expand"
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    if (isLabelsExpanded) {
                        if (labels.isEmpty() && isLoading) {
                             item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                             }
                        } else {
                            items(labels.filter { it.type == "user" }) { label ->
                                NavigationDrawerItem(
                                    label = { Text(text = label.name) },
                                    selected = false,
                                    onClick = { 
                                        onLabelClick?.invoke(label.id)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { 
                                         Icon(
                                             imageVector = Icons.Rounded.KeyboardArrowRight,
                                             contentDescription = null,
                                             modifier = Modifier.size(18.dp),
                                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                                         ) 
                                    },
                                    modifier = Modifier
                                        .padding(start = 24.dp) // Indent children
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .height(48.dp)
                                )
                            }
                        }
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Important") },
                            selected = false,
                            onClick = {
                                onLabelClick?.invoke("IMPORTANT")
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Star, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            label = { Text("Cleanup Assistant") },
                            selected = false,
                            onClick = {
                                onCleanupClick?.invoke()
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Build, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                HorizontalDivider()

                // Settings Button
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick?.invoke()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Logout Button
                NavigationDrawerItem(
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSignOutClick()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                // Add padding for navigation bar
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Gradient Box for Toolbar
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
                        .statusBarsPadding() // This pushes content down to respect status bar
                ) {
                    if (isSelectionMode) {
                        TopAppBar(
                            title = { Text("${selectedEmailIds.size} Selected") },
                            navigationIcon = {
                                IconButton(onClick = { selectedEmailIds = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Selection")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color.Black,
                                actionIconContentColor = Color.Black,
                                navigationIconContentColor = Color.Black
                            ),
                            actions = {
                                TextButton(onClick = {
                                    if (selectedEmailIds.size == emails.size) {
                                        selectedEmailIds = emptySet()
                                    } else {
                                        selectedEmailIds = emails.map { it.id }.toSet()
                                    }
                                }) {
                                    Text(
                                        text = if (selectedEmailIds.size == emails.size) "Deselect All" else "Select All",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (onDeleteEmails != null) {
                                    IconButton(onClick = {
                                        onDeleteEmails(selectedEmailIds.toList())
                                        selectedEmailIds = emptySet()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                                    }
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            title = { 
                                Text(
                                    text = if (showCategories) "OrganizeEmail" else title,
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            },
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
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            actions = {
                                // Removed direct logout from top bar, moved to drawer
                            }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GradientBlueStart.copy(alpha = 0.05f),
                                GradientPinkEnd.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                if (isLoading) {
                    LoadingSyncView(progress = syncProgress)
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    if (showCategories && onCategoryClick != null) {
                        CategoryCardGrid(
                            emails = emails,
                            onCategoryClick = onCategoryClick,
                            onSmartFilterClick = onSmartFilterClick
                        )
                    } else if (onSenderClick != null && !showAllEmails) {
                        SenderCardGrid(emails = emails, onSenderClick = onSenderClick)
                    } else {
                        if (emails.isEmpty()) {
                            EmptyStateView()
                        } else {
                            LazyColumn {
                                items(emails) { email ->
                                    val isSelected = selectedEmailIds.contains(email.id)
                                    EmailItem(
                                        email = email,
                                        isSelected = isSelected,
                                        inSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedEmailIds = if (isSelected) selectedEmailIds - email.id else selectedEmailIds + email.id
                                            } else {
                                                onEmailClick(email)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                selectedEmailIds = selectedEmailIds + email.id
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_mails),
            contentDescription = "No Emails",
            modifier = Modifier.size(240.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CategoryCardGrid(
    emails: List<EmailUI>,
    onCategoryClick: (String) -> Unit,
    onSmartFilterClick: ((String) -> Unit)? = null
) {
    val groups = emails.groupBy { it.category }.toList().sortedByDescending { it.second.size }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Smart Filters Section
        if (onSmartFilterClick != null) {
            item(span = { GridItemSpan(2) }) {
                SmartFiltersSection(emails = emails, onSmartFilterClick = onSmartFilterClick)
            }
            
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(groups) { (category, list) ->
            CategoryCard(category = category, emails = list, onClick = { onCategoryClick(category) })
        }
    }
}

@Composable
fun SmartFiltersSection(
    emails: List<EmailUI>,
    onSmartFilterClick: (String) -> Unit
) {
    val unreadCount = emails.count { it.isUnread }
    val attachmentCount = emails.count { it.hasMeaningfulAttachment }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Smart Filters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmartFilterCard(
                title = "Unread",
                count = unreadCount,
                icon = AppIcon.Vector(Icons.Default.Email),
                color = MaterialTheme.colorScheme.primary,
                onClick = { onSmartFilterClick("unread") },
                modifier = Modifier.weight(1f)
            )
            
            SmartFilterCard(
                title = "Attachments",
                count = attachmentCount,
                icon =  AppIcon.PainterIcon(painterResource(R.drawable.ic_attachement)),
                color = MaterialTheme.colorScheme.tertiary,
                onClick = { onSmartFilterClick("attachments") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SmartFilterCard(
    title: String,
    count: Int,
    icon: AppIcon,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (icon) {
                is AppIcon.Vector -> {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                is AppIcon.PainterIcon -> {
                    Icon(
                        painter = icon.painter,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: String, emails: List<EmailUI>, onClick: () -> Unit) {
    val gradientColors = getCategoryGradient(category)
    val senders = remember(emails) {
        emails.distinctBy { it.senderKey }.take(4)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background with low alpha - REMOVED per user request
            // Kept plain white/transparent inside the card as container is already white

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
                        .background(
                             brush = Brush.linearGradient(colors = gradientColors.map { it.copy(alpha = 0.1f) })
                        ),
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
}

fun getCategoryGradient(category: String): List<Color> {
    return when (category) {
        "Finance" -> listOf(Color(0xFF43A047), Color(0xFF66BB6A)) // Green
        "Jobs" -> listOf(Color(0xFF1976D2), Color(0xFF42A5F5)) // Blue
        "Shopping" -> listOf(Color(0xFFFB8C00), Color(0xFFFFB74D)) // Orange
        "Travel" -> listOf(Color(0xFF00ACC1), Color(0xFF26C6DA)) // Cyan
        "Social" -> listOf(Color(0xFFD81B60), Color(0xFFEC407A)) // Pink
        "Tech" -> listOf(Color(0xFF546E7A), Color(0xFF78909C)) // Blue Grey
        "Entertainment" -> listOf(Color(0xFF8E24AA), Color(0xFFAB47BC)) // Purple
        else -> listOf(Color(0xFF757575), Color(0xFF9E9E9E)) // Grey
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
    // Deprecated in favor of gradients, but kept for compatibility if needed elsewhere
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailItem(
    email: EmailUI,
    isSelected: Boolean = false,
    inSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val senderName = email.sender.substringBefore("<").trim()
    val randomColor = remember(email.senderKey) { getColorForName(email.senderKey) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Selection Indicator or Sender Avatar
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            // Existing Avatar Logic
            if (email.senderDomain != null) {
                AsyncImage(
                    model = "https://www.google.com/s2/favicons?domain=${email.senderDomain}&sz=64",
                    contentDescription = senderName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.AccountCircle)
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
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Email Content (Same as before)
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
fun LoadingSyncView(progress: Float = 0f) {
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GradientBlueStart.copy(alpha = 0.5f),
                            GradientBlueEnd.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Syncing",
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { this.alpha = alpha },
                tint = Color.Black // Match toolbar icon color
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
            text = "Organizing your inbox: ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = GradientBlueStart, // Match toolbar theme color
            trackColor = GradientBlueEnd.copy(alpha = 0.3f),
        )
    }
}
