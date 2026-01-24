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

        setContent {
            CallFeedbackTheme {
                FeedbackScreen()
            }
        }
    }
}
