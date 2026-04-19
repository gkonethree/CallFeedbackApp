package com.example.callfeedback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import kotlinx.coroutines.delay

/**
 * Foreground service that observes phone call state and collects post-call feedback metadata.
 *
 * The service keeps a low-priority ongoing notification to satisfy background execution
 * requirements and switches notification text while a call is active.
 */
class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CALL_MONITOR_SERVICE"
        private const val CHANNEL_ID = "call_monitor_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var callStateObserver: CallStateObserver
    private var isForegroundNotificationShown = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Builds the default foreground notification shown while waiting for call events.
     */
    private fun createMinimalNotification(): Notification {
        ensureChannelExists(getSystemService(NotificationManager::class.java))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call monitor active")
            .setContentText("Waiting for calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    /**
     * Updates the ongoing notification while an active call is detected.
     */
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
                try {
                    OverlayHelper.removeOverlay(this)
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to remove overlay on call start", t)
                }

                updateToInCallNotification()
            },
            onCallEnd = {
                callDuration:Long ->
                // Restore idle notification content after call completion.
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, createMinimalNotification())
                collectAndHandleCallEnd(callDuration)
            }
        )

        callStateObserver.start()
    }


    /**
     * Ensures call observation remains active if the service is restarted.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!::callStateObserver.isInitialized){
            callStateObserver.start()
        }
        return START_STICKY
    }

    /**
     * Stops telephony listeners, cancels pending work, and clears notifications/overlay.
     */
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


    /**
     * Creates the notification channel if it does not already exist.
     */
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

    @Suppress("unused")
    private fun launchFeedbackScreen() {
        val intent = Intent(this, FeedbackActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    /**
     * Submits collected feedback asynchronously to the backend repository.
     */
    private fun submitFeedbackToRepository(feedback: UserFeedback) {

        val repository = FeedbackRepository()
        serviceScope.launch {
            try {
                val result = repository.submitFeedback(feedback)

                if (result.isSuccess) {
                } else {
                    Log.e(TAG, "Failed to submit feedback to backend", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting feedback to backend", e)
            }
        }
    }

    /**
     * Collects metadata at call end and forwards it to the feedback UI flow.
     */
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

    /**
     * Shows the feedback overlay when allowed; otherwise submits metadata-only feedback.
     */
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
            OverlayHelper.removeOverlay(this)
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
            serviceScope.launch {
                delay(60_000)
                try {
                    OverlayHelper.removeOverlay(this@CallMonitorService)
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to auto-remove overlay", t)
                }
            }

        } catch (t: Throwable) {
            submitFeedbackToRepository(baseFeedback)
        }
    }
}