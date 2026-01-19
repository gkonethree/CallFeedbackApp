package com.example.callfeedback.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callfeedback.R
import com.example.callfeedback.telephony.CallStateObserver

class CallMonitorService : Service() {

    override fun onCreate() {
        super.onCreate()

        startForeground(1, buildNotification())

        val observer = CallStateObserver(
            this,
            onCallStart = { Log.d("CALL", "Call started") },
            onCallEnd = { Log.d("CALL", "Call ended") }
        )
        observer.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "call_channel")
            .setContentTitle("Call Monitoring Active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}
