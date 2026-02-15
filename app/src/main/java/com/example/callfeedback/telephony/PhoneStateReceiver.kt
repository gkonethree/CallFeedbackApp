package com.example.callfeedback.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import com.example.callfeedback.service.CallMonitorService

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == "android.intent.action.PHONE_STATE") {

            val state = intent.getStringExtra("state")

            if (state == TelephonyManager.EXTRA_STATE_RINGING ||
                state == TelephonyManager.EXTRA_STATE_OFFHOOK) {

                val serviceIntent = Intent(context, CallMonitorService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

}
