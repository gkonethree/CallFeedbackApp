package com.example.callfeedback.ui.feedback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.callfeedback.ui.theme.CallFeedbackTheme

class FeedbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefillRating = intent?.getIntExtra("prefill_rating", 3) ?: 3
        val prefillComment = intent?.getStringExtra("prefill_comment") ?: ""

        setContent {
            CallFeedbackTheme {
                FeedbackScreen(
                    initialRating = prefillRating,
                    initialComment = prefillComment
                )
            }
        }
    }
}
