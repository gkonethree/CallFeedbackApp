package com.example.callfeedback.telephony

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

@Suppress("DEPRECATION")

class CallStateObserver(
    context: Context,
    private val onCallStart: () -> Unit,
    private val onCallEnd: () -> Unit
) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun start() {
        telephonyManager.listen(
            object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_OFFHOOK -> onCallStart()
                        TelephonyManager.CALL_STATE_IDLE -> onCallEnd()
                    }
                }
            },
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }
}