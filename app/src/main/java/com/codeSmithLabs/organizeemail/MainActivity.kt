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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codeSmithLabs.organizeemail.data.model.EmailUI
import com.codeSmithLabs.organizeemail.ui.email.EmailDetailScreen
import com.codeSmithLabs.organizeemail.ui.email.EmailListScreen
import com.codeSmithLabs.organizeemail.ui.login.LoginScreen
import com.codeSmithLabs.organizeemail.ui.theme.OrganizeEmailTheme
import com.codeSmithLabs.organizeemail.ui.viewmodel.EmailViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

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
                            showCategories = true,
                            showAllEmails = false
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
                                navController.navigate("sender_list/$key")
                            },
                            showCategories = false,
                            showAllEmails = false,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("sender_list/{key}") { backStackEntry ->
                        val key = backStackEntry.arguments?.getString("key") ?: ""
                        val filtered = emails.filter { it.senderKey == key }
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
                        EmailDetailScreen(
                            email = selectedEmail,
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
