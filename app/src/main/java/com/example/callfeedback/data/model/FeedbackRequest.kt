package com.example.callfeedback.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    val rating: Int? = null,  // Voice quality rating 1-5
    val audioIssues: List<String>? = null,  // List of audio issue tags
    val environment: String? = null,  // Environment type (indoor, outdoor, in_vehicle, noisy_area)
    val comment: String? = null  // User comments
)
