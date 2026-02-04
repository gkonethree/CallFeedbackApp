package com.example.callfeedback.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.Toast
import android.widget.ToggleButton
import android.widget.CheckBox
import androidx.appcompat.view.ContextThemeWrapper
import com.example.callfeedback.R
import com.example.callfeedback.data.repository.FeedbackRepository
import com.example.callfeedback.ui.feedback.FeedbackActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        // Voice quality rating - using discrete stars
        val stars = listOf(
            layout.findViewById<ImageButton>(R.id.star_1),
            layout.findViewById<ImageButton>(R.id.star_2),
            layout.findViewById<ImageButton>(R.id.star_3),
            layout.findViewById<ImageButton>(R.id.star_4),
            layout.findViewById<ImageButton>(R.id.star_5)
        )

        var selectedRating = 0

        // Define updateStars function before using it
        fun updateStars(starList: List<ImageButton?>, rating: Int) {
            starList.forEachIndexed { index, star ->
                if (index < rating) {
                    star?.setImageResource(R.drawable.ic_star_filled)
                } else {
                    star?.setImageResource(R.drawable.ic_star_empty)
                }
            }
        }

        // Set up star click listeners
        stars.forEachIndexed { index, star ->
            star?.setOnClickListener {
                selectedRating = index + 1
                updateStars(stars, selectedRating)
            }
        }

        // Audio issues toggle buttons
        val audioIssueDropped = layout.findViewById<ToggleButton>(R.id.audio_issue_dropped)
        val audioIssueHearOther = layout.findViewById<ToggleButton>(R.id.audio_issue_hear_other)
        val audioIssueHearMe = layout.findViewById<ToggleButton>(R.id.audio_issue_hear_me)
        val audioIssueBackgroundNoise = layout.findViewById<ToggleButton>(R.id.audio_issue_background_noise)
        val audioIssueEcho = layout.findViewById<ToggleButton>(R.id.audio_issue_echo)

        // Set text colors for audio issue buttons based on checked state
        val audioButtons = listOf(audioIssueDropped, audioIssueHearOther, audioIssueHearMe, audioIssueBackgroundNoise, audioIssueEcho)
        audioButtons.forEach { button ->
            button?.setOnCheckedChangeListener { _, isChecked ->
                button.setTextColor(if (isChecked) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            }
            // Set initial color (black for unchecked)
            button?.setTextColor(if (button.isChecked) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
        }

        // Environment checkboxes (single selection)
        val envIndoor = layout.findViewById<CheckBox>(R.id.env_indoor)
        val envOutdoor = layout.findViewById<CheckBox>(R.id.env_outdoor)
        val envVehicle = layout.findViewById<CheckBox>(R.id.env_vehicle)
        val envNoisyArea = layout.findViewById<CheckBox>(R.id.env_noisy_area)

        val environmentCheckboxes = listOf(envIndoor, envOutdoor, envVehicle, envNoisyArea)

        // Set up single-selection logic for environment
        environmentCheckboxes.forEach { checkbox ->
            checkbox?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    environmentCheckboxes.forEach { other ->
                        if (other != checkbox) {
                            other?.isChecked = false
                        }
                    }
                }
            }
        }

        val commentInput = layout.findViewById<EditText>(R.id.overlay_comment)
        val closeBtn = layout.findViewById<ImageButton>(R.id.overlay_close)
        val submitBtn = layout.findViewById<Button>(R.id.overlay_submit)
        val rootView = layout.findViewById<View>(R.id.overlay_root)

        closeBtn?.setOnClickListener {
            removeOverlay(context)
        }

        // Close overlay when tapping outside the card
        rootView?.setOnClickListener {
            removeOverlay(context)
        }

        // Prevent closing when tapping on the card itself
        layout.findViewById<View>(R.id.overlay_container)?.setOnClickListener {
            // Do nothing - consume the click
        }

        submitBtn?.setOnClickListener {
            // Collect voice quality (1-5, null if not set)
            val voiceQuality = if (selectedRating > 0) selectedRating else null

            // Collect audio issues
            val audioIssues = mutableListOf<String>()
            if (audioIssueDropped?.isChecked == true) audioIssues.add("call_dropped")
            if (audioIssueHearOther?.isChecked == true) audioIssues.add("could_not_hear_other")
            if (audioIssueHearMe?.isChecked == true) audioIssues.add("other_could_not_hear_me")
            if (audioIssueBackgroundNoise?.isChecked == true) audioIssues.add("background_noise")
            if (audioIssueEcho?.isChecked == true) audioIssues.add("echo")

            // Collect environment (single selection)
            val environment = when {
                envIndoor?.isChecked == true -> "indoor"
                envOutdoor?.isChecked == true -> "outdoor"
                envVehicle?.isChecked == true -> "in_vehicle"
                envNoisyArea?.isChecked == true -> "noisy_area"
                else -> null
            }

            val comment = commentInput?.text?.toString()?.trim()

            submitBtn.isEnabled = false

            Log.d(TAG, "Submitting feedback - Voice Quality: $voiceQuality, Audio Issues: $audioIssues, Environment: $environment, Comment: $comment")

            // Submit to backend
            val repository = FeedbackRepository()
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = repository.submitFeedback(voiceQuality, audioIssues, environment, comment)
                    if (result.isSuccess) {
                        Log.d(TAG, "Feedback submitted to backend successfully")
                        Toast.makeText(context, "Feedback submitted", Toast.LENGTH_SHORT).show()
                        removeOverlay(context)
                    } else {
                        Log.e(TAG, "Failed to submit feedback", result.exceptionOrNull())
                        Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                        submitBtn.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error submitting feedback", e)
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    submitBtn.isEnabled = true
                }
            }
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
