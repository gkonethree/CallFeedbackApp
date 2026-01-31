package com.example.callfeedback.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import androidx.appcompat.view.ContextThemeWrapper
import com.example.callfeedback.R
import com.example.callfeedback.ui.feedback.FeedbackActivity
import java.lang.ref.WeakReference

object OverlayHelper {

    private const val TAG = "OverlayHelper"

    @SuppressLint("StaticFieldLeak")
    private var overlayViewRef: WeakReference<View?> = WeakReference(null)

    private var overlayView: View?
        get() = overlayViewRef.get()
        set(value) { overlayViewRef = WeakReference(value) }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermissionIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun showOverlay(context: Context) {
        if (overlayView != null) {
            Log.d(TAG, "overlay already shown")
            return
        }

        if (!canDrawOverlays(context)) {
            Log.w(TAG, "no overlay permission")
            return
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Use the app theme to inflate AppCompat views correctly in an overlay window
        val themedContext = ContextThemeWrapper(context, R.style.Theme_CallFeedback)
        val inflater = LayoutInflater.from(themedContext)

        val layout = inflater.inflate(R.layout.overlay_feedback, null)

        // rating and controls
        val ratingVoice = layout.findViewById<RatingBar>(R.id.rating_voice_quality)
        val ratingDelays = layout.findViewById<RatingBar>(R.id.rating_delays)
        val ratingNetwork = layout.findViewById<RatingBar>(R.id.rating_network_reliability)
        val commentInput = layout.findViewById<EditText>(R.id.overlay_comment)
        val closeBtn = layout.findViewById<ImageButton>(R.id.overlay_close)
        val submitBtn = layout.findViewById<Button>(R.id.overlay_submit)
        val container = layout.findViewById<View>(R.id.overlay_container)

        // defaults
        ratingVoice?.rating = 5f
        ratingDelays?.rating = 5f
        ratingNetwork?.rating = 5f

        closeBtn?.setOnClickListener {
            removeOverlay(context)
        }

        submitBtn?.setOnClickListener {
            val vRating = ratingVoice?.rating?.toInt()?.coerceIn(1, 5) ?: 5
            val dRating = ratingDelays?.rating?.toInt()?.coerceIn(1, 5) ?: 5
            val nRating = ratingNetwork?.rating?.toInt()?.coerceIn(1, 5) ?: 5
            val comment = commentInput.text?.toString()?.trim().orEmpty()

            val intent = Intent(context, FeedbackActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("prefill_voice_rating", vRating)
                putExtra("prefill_delays_rating", dRating)
                putExtra("prefill_network_rating", nRating)
                putExtra("prefill_comment", comment)
            }
            context.startActivity(intent)
            removeOverlay(context)
        }

        val params = WindowManager.LayoutParams().apply {
            // full-screen so root view can detect taps outside the card
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER
            x = 0
            y = 0
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }

        try {
            windowManager.addView(layout, params)
            overlayView = layout
        } catch (t: Throwable) {
            Log.w(TAG, "failed to add overlay", t)
        }
    }

    fun removeOverlay(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (t: Throwable) {
                Log.w(TAG, "failed to remove overlay", t)
            }
            overlayView = null
        }
    }
}
