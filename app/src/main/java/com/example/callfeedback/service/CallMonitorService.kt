package com.example.callfeedback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callfeedback.R
import com.example.callfeedback.telephony.CallStateObserver
import com.example.callfeedback.ui.feedback.FeedbackActivity

class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CALL_MONITOR_SERVICE"
        private const val CHANNEL_ID = "call_monitor_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var callStateObserver: CallStateObserver
    private var isForegroundNotificationShown = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Clear any leftover notifications from previous runs to avoid always-visible notifications
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        manager?.cancel(NOTIFICATION_ID + 1)

        callStateObserver = CallStateObserver(
            context = this,
            onCallStart = {
                Log.d(TAG, "Call started")
                // Only enter foreground if not already in foreground
                if (!isForegroundNotificationShown) {
                    startForeground(NOTIFICATION_ID, createInCallNotification())
                    isForegroundNotificationShown = true
                }
            },
            onCallEnd = {
                Log.d(TAG, "Call ended")
                // Remove the in-call foreground notification if shown, then post feedback notification
                if (isForegroundNotificationShown) {
                    stopForeground(true)
                    isForegroundNotificationShown = false
                } else {
                    // If we didn't show the foreground then ensure any stale notifications are removed
                    val mgr = getSystemService(NotificationManager::class.java)
                    mgr?.cancel(NOTIFICATION_ID)
                }

                // Try to show overlay if permission granted; otherwise fall back to notification
                try {
                    val overlayAllowed = com.example.callfeedback.service.OverlayHelper.canDrawOverlays(this)
                    if (overlayAllowed) {
                        //log
                        Log.d(TAG, "Showing overlay for feedback")
                        com.example.callfeedback.service.OverlayHelper.showOverlay(this)
                    } else {
                        notifyFeedbackAvailable()
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Overlay failed, falling back to notification", t)
                    notifyFeedbackAvailable()
                }
            }
        )

        callStateObserver.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        callStateObserver.stop()
        // Clean up notifications
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        manager?.cancel(NOTIFICATION_ID + 1)
        // Ensure overlay removed
        com.example.callfeedback.service.OverlayHelper.removeOverlay(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------
    // Notification helpers
    // -------------------------

    private fun ensureChannelExists(manager: NotificationManager?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun createInCallNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        ensureChannelExists(manager)

        val feedbackIntent = Intent(this, FeedbackActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, feedbackIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("In call")
            .setContentText("Tap to open feedback after call")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun notifyFeedbackAvailable() {
        val manager = getSystemService(NotificationManager::class.java)
        ensureChannelExists(manager)

        val feedbackIntent = Intent(this, FeedbackActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, feedbackIntent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Share call feedback")
            .setContentText("Tap to provide feedback about your recent call")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager?.notify(NOTIFICATION_ID + 1, notification)
    }

    @Suppress("unused")
    private fun launchFeedbackScreen() {
        val intent = Intent(this, FeedbackActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}
