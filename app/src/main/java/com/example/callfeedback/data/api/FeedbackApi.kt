package com.example.callfeedback.data.api

import com.example.callfeedback.data.model.UserFeedback
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http. Header

interface FeedbackApi {
    @POST("feedback")
    suspend fun submitFeedback(@Header("X-API-KEY") apiKey:String,@Body request: UserFeedback)
}
