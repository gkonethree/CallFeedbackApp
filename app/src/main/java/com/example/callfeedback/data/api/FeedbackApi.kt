package com.example.callfeedback.data.api

import com.example.callfeedback.data.model.FeedbackRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackApi {
    @POST("feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest)
}
