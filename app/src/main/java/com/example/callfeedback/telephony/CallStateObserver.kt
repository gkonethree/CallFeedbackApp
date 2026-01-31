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
    private val onCallEnd: () -> Unit
) {

    companion object {
        private const val TAG = "CALL_STATE_OBSERVER"
    }

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var wasInCall = false
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

            // If we start while already in a call, mark observedCallStart so we will
            // still emit onCallEnd when the call finishes. This covers the case where
            // the app/service was started mid-call.
            observedCallStart = wasInCall
            Log.d(TAG, "Initial call state: $initialState, wasInCall=$wasInCall, observedCallStart=$observedCallStart")
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
    }

    private fun handleCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "Phone ringing")
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!wasInCall) {
                    wasInCall = true
                    observedCallStart = true
                    Log.d(TAG, "Call started (observed)")
                    onCallStart()
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasInCall) {
                    wasInCall = false
                    if (observedCallStart) {
                        // Only emit call end for calls we observed the start of
                        observedCallStart = false
                        Log.d(TAG, "Call ended (observed)")
                        onCallEnd()
                    } else {
                        Log.d(TAG, "Call ended but start was not observed — ignoring")
                    }
                }
            }
        }
    }
}
