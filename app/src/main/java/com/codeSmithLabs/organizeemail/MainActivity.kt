package com.codeSmithLabs.organizeemail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import com.codeSmithLabs.organizeemail.ui.email.EmailDetailScreen
import com.codeSmithLabs.organizeemail.ui.email.EmailListScreen
import com.codeSmithLabs.organizeemail.ui.login.LoginScreen
import com.codeSmithLabs.organizeemail.ui.settings.SettingsScreen
import com.codeSmithLabs.organizeemail.ui.theme.OrganizeEmailTheme
import com.codeSmithLabs.organizeemail.ui.viewmodel.EmailViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val viewModel: EmailViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.handleSignInResult(account)
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrganizeEmailTheme {
                val navController = rememberNavController()
                val user by viewModel.user.collectAsState()
                val emails by viewModel.emails.collectAsState()
                val labels by viewModel.labels.collectAsState()
                val isLoading by viewModel.loading.collectAsState()
                val error by viewModel.error.collectAsState()

                // State to hold the selected email for detail view
                var selectedEmail by remember { mutableStateOf<EmailUI?>(null) }

                NavHost(
                    navController = navController,
                    startDestination = if (user == null) "login" else "email_list"
                ) {
                    composable("login") {
                        if (user != null) {
                            navController.navigate("email_list") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        LoginScreen(
                            onSignInClick = {
                                val signInIntent = viewModel.getAuthClient().getSignInIntent()
                                signInLauncher.launch(signInIntent)
                            }
                        )
                    }
                    composable("email_list") {
                        // Ensure we are showing Inbox data when on the main screen
                        LaunchedEffect(Unit) {
                            // Only fetch if we are NOT already showing inbox (optimization)
                            // But since fetchEmails handles cache efficiently, we can just call it with null
                            viewModel.fetchEmails(labelId = null)
                        }

                        if (user == null) {
                            navController.navigate("login") {
                                popUpTo("email_list") { inclusive = true }
                            }
                        }
                        EmailListScreen(
                            emails = emails,
                            labels = labels,
                            user = user,
                            isLoading = isLoading,
                            error = error,
                            onEmailClick = { email ->
                                selectedEmail = email
                                navController.navigate("email_detail")
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo("email_list") { inclusive = true }
                                }
                            },
                            title = "OrganizeEmail",
                            onSenderClick = { key ->
                                navController.navigate("sender_list/$key")
                            },
                            onCategoryClick = { category ->
                                navController.navigate("category_list/$category")
                            },
                            onLabelClick = { labelId ->
                                viewModel.fetchEmails(labelId = labelId)
                                navController.navigate("label_list/$labelId")
                            },
                            onSmartFilterClick = { type ->
                                navController.navigate("smart_filter/$type")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            showCategories = true,
                            showAllEmails = false
                        )
                    }
                    composable("smart_filter/{type}") { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        val title = when (type) {
                            "unread" -> "Unread Emails"
                            "attachments" -> "Emails with Attachments"
                            else -> "Filtered Emails"
                        }
                        
                        val filtered = when (type) {
                            "unread" -> emails.filter { it.isUnread }
                            "attachments" -> emails.filter { it.hasMeaningfulAttachment }
                            else -> emptyList()
                        }
                        
                        EmailListScreen(
                            emails = filtered,
                            labels = emptyList(),
                            user = user,
                            isLoading = false,
                            error = null,
                            onEmailClick = { email ->
                                selectedEmail = email
                                navController.navigate("email_detail")
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo("email_list") { inclusive = true }
                                }
                            },
                            title = title,
                            onSenderClick = { key ->
                                navController.navigate("sender_list/$key")
                            },
                            showCategories = false,
                            showAllEmails = false,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("label_list/{labelId}") { backStackEntry ->
                        val labelId = backStackEntry.arguments?.getString("labelId") ?: ""
                        val labelName = labels.find { it.id == labelId }?.name ?: "Label"
                        
                        EmailListScreen(
                            emails = emails,
                            labels = labels,
                            user = user,
                            isLoading = isLoading,
                            error = error,
                            onEmailClick = { email ->
                                selectedEmail = email
                                navController.navigate("email_detail")
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo("email_list") { inclusive = true }
                                }
                            },
                            title = labelName,
                            onSenderClick = { key ->
                                navController.navigate("sender_list/$key")
                            },
                            onCategoryClick = { category ->
                                navController.navigate("category_list/$category")
                            },
                            onLabelClick = { newLabelId ->
                                if (newLabelId != labelId) {
                                    viewModel.fetchEmails(labelId = newLabelId)
                                    navController.navigate("label_list/$newLabelId") {
                                        popUpTo("email_list") { inclusive = false }
                                    }
                                }
                            },
                            showCategories = false,
                            showAllEmails = true,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("category_list/{category}") { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: ""
                        val filtered = emails.filter { it.category == category }
                        EmailListScreen(
                            emails = filtered,
                            labels = emptyList(),
                            user = user,
                            isLoading = false,
                            error = null,
                            onEmailClick = { email ->
                                selectedEmail = email
                                navController.navigate("email_detail")
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo("email_list") { inclusive = true }
                                }
                            },
                            title = category,
                            onSenderClick = { key ->
                                navController.navigate("sender_list/$key?category=$category")
                            },
                            showCategories = false,
                            showAllEmails = false,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "sender_list/{key}?category={category}",
                        arguments = listOf(
                            navArgument("key") { type = NavType.StringType },
                            navArgument("category") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val key = backStackEntry.arguments?.getString("key") ?: ""
                        val category = backStackEntry.arguments?.getString("category")
                        
                        val filtered = emails.filter { 
                            it.senderKey == key && (category == null || it.category == category)
                        }
                        EmailListScreen(
                            emails = filtered,
                            labels = emptyList(),
                            user = user,
                            isLoading = false,
                            error = null,
                            onEmailClick = { email ->
                                selectedEmail = email
                                navController.navigate("email_detail")
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo("email_list") { inclusive = true }
                                }
                            },
                            title = key,
                            onSenderClick = null,
                            showAllEmails = true,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("email_detail") {
                        val context = LocalContext.current
                        EmailDetailScreen(
                            email = selectedEmail,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onDownloadAttachment = { attachment ->
                                selectedEmail?.let { email ->
                                    Toast.makeText(context, "Downloading ${attachment.filename}...", Toast.LENGTH_SHORT).show()
                                    viewModel.downloadAttachment(email.id, attachment) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Downloaded ${attachment.filename}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to download ${attachment.filename}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
