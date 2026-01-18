package com.codeSmithLabs.organizeemail.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks

class GoogleAuthClient(private val context: Context) {

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/gmail.modify"))
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                Tasks.await(googleSignInClient.signOut())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSignedInAccountFromIntent(data: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            e.printStackTrace()
            null
        }
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                val scope = "oauth2:https://www.googleapis.com/auth/gmail.modify"
                GoogleAuthUtil.getToken(context, account.account!!, scope)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
