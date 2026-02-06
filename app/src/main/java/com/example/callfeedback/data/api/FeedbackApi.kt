package com.example.callfeedback.data.api

import com.example.callfeedback.data.model.UserFeedback
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackApi {
    @POST("feedback")
    suspend fun submitFeedback(@Body request: UserFeedback)
}
