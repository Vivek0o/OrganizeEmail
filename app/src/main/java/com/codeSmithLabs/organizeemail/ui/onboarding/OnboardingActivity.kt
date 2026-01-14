package com.codeSmithLabs.organizeemail.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codeSmithLabs.organizeemail.MainActivity
import com.codeSmithLabs.organizeemail.ui.theme.OrganizeEmailTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrganizeEmailTheme {
                OnboardingScreen(
                    onFinish = {
                        // Navigate to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        // Clear the back stack so user can't go back to onboarding
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
