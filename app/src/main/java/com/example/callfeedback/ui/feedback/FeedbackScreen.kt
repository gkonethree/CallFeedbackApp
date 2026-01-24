package com.example.callfeedback.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedbackScreen() {

    var rating by remember { mutableStateOf(3f) }
    var comment by remember { mutableStateOf("") }

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
                // TODO: save feedback
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Feedback")
        }
    }
}
