package com.example.callfeedback.ui.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Feedback form used to capture a call quality rating and optional comment.
 *
 * @param initialRating Initial slider value (1..5 range expected by the UI).
 * @param initialComment Pre-filled comment text.
 * @param onSubmit Invoked when the user submits feedback with the selected rating/comment.
 */
@Composable
fun FeedbackScreen(
    initialRating: Int = 3,
    initialComment: String = "",
    onSubmit: ((rating: Int, comment: String) -> Unit)? = null
) {

    var rating by remember { mutableStateOf(initialRating.toFloat()) }
    var comment by remember { mutableStateOf(initialComment) }
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Call Feedback",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("How was your call quality?")

        Slider(
            value = rating,
            onValueChange = { rating = it },
            valueRange = 1f..5f,
            steps = 3
        )

        Text("Rating: ${rating.toInt()} / 5")

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Additional comments") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSubmit?.invoke(rating.toInt(), comment)
                Toast.makeText(ctx, "Feedback queued", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Feedback")
        }
    }
}
