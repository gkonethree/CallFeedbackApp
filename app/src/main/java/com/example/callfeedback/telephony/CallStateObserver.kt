package com.example.callfeedback.telephony

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import java.util.concurrent.Executor

@Suppress("DEPRECATION")
class CallStateObserver(
    context: Context,
    private val onCallStart: () -> Unit,
    private val onCallEnd: (callDuration:Long) -> Unit
) {

    companion object {
        private const val TAG = "CALL_STATE_OBSERVER"
    }

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var wasInCall = false
    private var callStartTime: Long = 0
    private var observedCallStart = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    private val mainExecutor: Executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.mainExecutor
    } else {
        Executor { runnable -> Handler(Looper.getMainLooper()).post(runnable) }
    }

    fun start() {
        try {
            val initialState = telephonyManager.callState
            wasInCall = (initialState == TelephonyManager.CALL_STATE_OFFHOOK)

            observedCallStart = wasInCall

        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read initial call state", t)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallStateChanged(state)
                }
            }
            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
        } else {

            phoneStateListener = object : PhoneStateListener() {
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallStateChanged(state)
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        } else {
            phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
            phoneStateListener = null
        }
        // Reset observed state when stopping
        observedCallStart = false
        wasInCall=false
    }

    private fun handleCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!wasInCall) {
                    wasInCall = true
                    observedCallStart = true
                    callStartTime=System.currentTimeMillis()
                    onCallStart()
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasInCall) {
                    wasInCall = false
                    if (observedCallStart) {
                        val callDuration=System.currentTimeMillis()-callStartTime
                        observedCallStart = false
                        onCallEnd(callDuration)
                    } else {

                    }
                }
            }
        }
    }
}
