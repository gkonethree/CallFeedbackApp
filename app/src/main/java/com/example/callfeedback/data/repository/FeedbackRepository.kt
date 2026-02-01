package com.example.callfeedback.data.repository

import android.util.Log
import com.example.callfeedback.data.api.ApiClient
import com.example.callfeedback.data.model.FeedbackRequest

class FeedbackRepository {
    companion object {
        private const val TAG = "FeedbackRepository"
    }

    private suspend fun <T> retry(times: Int = 2, block: suspend () -> T): T {
        var last: Exception? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                android.util.Log.w(TAG, "submit attempt ${attempt + 1} failed: ${e.message}")
                kotlinx.coroutines.delay(800)
            }
        }
        throw last ?: IllegalStateException("unknown error")
    }

    suspend fun submitFeedback(
        voiceQuality: Int,
        delays: Int,
        networkReliability: Int,
        comment: String
    ): Result<Unit> = try {
        val request = FeedbackRequest(
            voiceQuality = voiceQuality,
            delays = delays,
            networkReliability = networkReliability,
            comment = comment
        )
        retry {
            ApiClient.feedbackApi.submitFeedback(request)
        }
        Log.d(TAG, "Feedback submitted successfully")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to submit feedback", e)
        Result.failure(e)
    }
}
