package com.example.callfeedback.data.repository
import com.example.callfeedback.BuildConfig
import android.util.Log
import com.example.callfeedback.data.api.ApiClient
import com.example.callfeedback.data.model.UserFeedback

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
                Log.w(TAG, "submit attempt ${attempt + 1} failed: ${e.message}")
                kotlinx.coroutines.delay(800)
            }
        }
        throw last ?: IllegalStateException("unknown error")
    }

    suspend fun submitFeedback(feedback: UserFeedback): Result<Unit> = try {
        retry {
            ApiClient.feedbackApi.submitFeedback(BuildConfig.API_KEY,feedback)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
