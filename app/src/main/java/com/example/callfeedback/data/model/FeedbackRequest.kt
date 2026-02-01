package com.example.callfeedback.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    val voiceQuality: Int,
    val delays: Int,
    val networkReliability: Int,
    val comment: String
)
