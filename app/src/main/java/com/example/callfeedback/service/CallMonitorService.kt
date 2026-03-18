package com.example.callfeedback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callfeedback.R
import com.example.callfeedback.data.repository.FeedbackRepository
import com.example.callfeedback.telephony.CallStateObserver
import com.example.callfeedback.data.model.UserFeedback
import com.example.callfeedback.ui.feedback.FeedbackActivity
import com.example.callfeedback.ui.overlay.OverlayHelper
import com.example.callfeedback.util.DeviceMetadataCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CALL_MONITOR_SERVICE"
        private const val CHANNEL_ID = "call_monitor_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var callStateObserver: CallStateObserver
    private var isForegroundNotificationShown = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private fun createMinimalNotification(): Notification {
        ensureChannelExists(getSystemService(NotificationManager::class.java))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call monitor active")
            .setContentText("Waiting for calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateToInCallNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("In call")
            .setContentText("Monitoring call quality")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }


    override fun onCreate() {
        super.onCreate()

        startForeground(NOTIFICATION_ID, createMinimalNotification())

        callStateObserver = CallStateObserver(
            context = this,
            onCallStart = {
                updateToInCallNotification()
            },
            onCallEnd = {
                callDuration:Long ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, createMinimalNotification())
                collectAndHandleCallEnd(callDuration)
            }
        )

        callStateObserver.start()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!::callStateObserver.isInitialized){
            callStateObserver.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        callStateObserver.stop()
        serviceScope.cancel()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        manager?.cancel(NOTIFICATION_ID + 1)

        OverlayHelper.removeOverlay(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun ensureChannelExists(manager: NotificationManager?) {

        manager ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

    }

//    private fun notifyFeedbackAvailable() {
//        val manager = getSystemService(NotificationManager::class.java)
//        ensureChannelExists(manager)
//
//        val feedbackIntent = Intent(this, FeedbackActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntentFlags =
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//
//        val pendingIntent = PendingIntent.getActivity(this, 0, feedbackIntent, pendingIntentFlags)
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Share call feedback")
//            .setContentText("Tap to provide feedback about your recent call")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .build()
//
//        manager?.notify(NOTIFICATION_ID + 1, notification)
//    }

    @Suppress("unused")
    private fun launchFeedbackScreen() {
        val intent = Intent(this, FeedbackActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun submitFeedbackToRepository(feedback: UserFeedback) {

        val repository = FeedbackRepository()
        serviceScope.launch {
            try {
                val result = repository.submitFeedback(feedback)

                if (result.isSuccess) {
                    Log.d(TAG, "Feedback submitted to backend successfully")
                } else {
                    Log.e(TAG, "Failed to submit feedback to backend", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting feedback to backend", e)
            }
        }
    }

    private fun collectAndHandleCallEnd(callDuration:Long) {
        val metadataCollector = DeviceMetadataCollector(this)
        val networkGeneration = metadataCollector.getNetworkGeneration()
        val signalStrength = metadataCollector.getSignalStrength()
        val timestamp = metadataCollector.getTimestamp()
        val carrier=metadataCollector.getCarrier()

        metadataCollector.getLocation { latitude, longitude ->
            showFeedbackUI(carrier,networkGeneration, signalStrength, latitude, longitude, timestamp,callDuration)
        }
    }

    private fun showFeedbackUI(
        carrier: String?,
        networkGeneration: String,
        signalStrength: Int?,
        latitude: Double?,
        longitude: Double?,
        timestamp: Long?,
        callDuration: Long?
    ) {

        val baseFeedback = UserFeedback(
            carrier = carrier,
            networkGeneration = networkGeneration,
            signalStrength = signalStrength,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            callDuration = callDuration
        )

        try {
            if (!OverlayHelper.canDrawOverlays(this)) {
                submitFeedbackToRepository(baseFeedback)
                return
            }

            OverlayHelper.showOverlay(this) { feedback ->

                val finalFeedback = feedback?.copy(
                    carrier = carrier,
                    networkGeneration = networkGeneration,
                    signalStrength = signalStrength,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = timestamp,
                    callDuration = callDuration
                ) ?: baseFeedback

                submitFeedbackToRepository(finalFeedback)
            }

        } catch (t: Throwable) {
            submitFeedbackToRepository(baseFeedback)
        }
    }
}