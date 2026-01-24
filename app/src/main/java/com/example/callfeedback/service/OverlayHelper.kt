package com.example.callfeedback.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import com.example.callfeedback.ui.feedback.FeedbackActivity
import java.lang.ref.WeakReference

object OverlayHelper {

    @SuppressLint("StaticFieldLeak")
    private var overlayViewRef: WeakReference<View?> = WeakReference(null)

    private var overlayView: View?
        get() = overlayViewRef.get()
        set(value) { overlayViewRef = WeakReference(value) }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermissionIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun showOverlay(context: Context) {
        // If already shown, do nothing
        if (overlayView != null) return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#DDFFFFFF"))
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            elevation = 8f
        }

        val title = TextView(context).apply {
            text = "Rate recent call"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        val rating = RatingBar(context).apply {
            numStars = 5
            stepSize = 1f
            rating = 3f
        }

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val closeBtn = Button(context).apply {
            text = "Close"
            setOnClickListener {
                removeOverlay(context)
            }
        }

        val feedbackBtn = Button(context).apply {
            text = "Give feedback"
            setOnClickListener {
                // Launch FeedbackActivity and remove overlay
                val intent = Intent(context, FeedbackActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
                removeOverlay(context)
            }
        }

        buttonRow.addView(closeBtn)
        buttonRow.addView(feedbackBtn)

        layout.addView(title)
        layout.addView(rating)
        layout.addView(buttonRow)

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            format = android.graphics.PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                // Deprecated but fallback
                type = WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }

        try {
            windowManager.addView(layout, params)
            overlayView = layout
        } catch (t: Throwable) {
            // Failed to add overlay (no permission or other error)
        }
    }

    fun removeOverlay(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (t: Throwable) {
                // ignore
            }
            overlayView = null
        }
    }
}
