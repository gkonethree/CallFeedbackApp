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
import android.widget.ToggleButton
import android.widget.CheckBox
import androidx.appcompat.view.ContextThemeWrapper
import com.example.callfeedback.R
import com.example.callfeedback.data.model.AudioIssue
import com.example.callfeedback.data.model.Environment
import com.example.callfeedback.data.model.UserFeedback
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

    fun showOverlay(
        context: Context,
        onFeedbackResult: ((feedback: UserFeedback?) -> Unit)? = null
    ) {
        if (overlayView != null) {

            return
        }

        if (!canDrawOverlays(context)) {
            Log.w(TAG, "no overlay permission")
            return
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val themedContext = ContextThemeWrapper(context, R.style.Theme_CallFeedback)
        val inflater = LayoutInflater.from(themedContext)
        val layout = inflater.inflate(R.layout.overlay_feedback, null)

        val stars = listOf(
            layout.findViewById<ImageButton>(R.id.star_1),
            layout.findViewById<ImageButton>(R.id.star_2),
            layout.findViewById<ImageButton>(R.id.star_3),
            layout.findViewById<ImageButton>(R.id.star_4),
            layout.findViewById<ImageButton>(R.id.star_5)
        )

        var selectedRating: Int? = null

        fun updateStars(starList: List<ImageButton?>, rating: Int?) {
            starList.forEachIndexed { index, star ->
                if (rating != null && index < rating) {
                    star?.setImageResource(R.drawable.ic_star_filled)
                } else {
                    star?.setImageResource(R.drawable.ic_star_empty)
                }
            }
        }

        stars.forEachIndexed { index, star ->
            star?.setOnClickListener {
                selectedRating = index + 1
                updateStars(stars, selectedRating)
            }
        }

        val audioPerfect = layout.findViewById<ToggleButton>(R.id.audio_perfect)
        val audioIssueDropped = layout.findViewById<ToggleButton>(R.id.audio_issue_dropped)
        val audioIssueHearOther = layout.findViewById<ToggleButton>(R.id.audio_issue_hear_other)
        val audioIssueHearMe = layout.findViewById<ToggleButton>(R.id.audio_issue_hear_me)
        val audioIssueBackgroundNoise = layout.findViewById<ToggleButton>(R.id.audio_issue_background_noise)
        val audioIssueEcho = layout.findViewById<ToggleButton>(R.id.audio_issue_echo)

        val audioButtons = listOf(audioPerfect,audioIssueDropped, audioIssueHearOther, audioIssueHearMe, audioIssueBackgroundNoise, audioIssueEcho)
        audioButtons.forEach { button ->
            button?.setOnCheckedChangeListener { _, isChecked ->
                button.setTextColor(if (isChecked) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            }
            button?.setTextColor(if (button.isChecked) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
        }

        val envIndoor = layout.findViewById<CheckBox>(R.id.env_indoor)
        val envOutdoor = layout.findViewById<CheckBox>(R.id.env_outdoor)
        val envVehicle = layout.findViewById<CheckBox>(R.id.env_vehicle)
        val envNoisyArea = layout.findViewById<CheckBox>(R.id.env_noisy_area)

        val environmentCheckboxes = listOf(envIndoor, envOutdoor, envVehicle, envNoisyArea)

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
            onFeedbackResult?.invoke(null)
            removeOverlay(context)
        }

        rootView?.setOnClickListener {

            onFeedbackResult?.invoke(null)
            removeOverlay(context)
        }


        layout.findViewById<View>(R.id.overlay_container)?.setOnClickListener {
            // Do nothing - consume the click
        }

        submitBtn?.setOnClickListener {
            val voiceQuality = selectedRating

            val audioIssues = mutableListOf<AudioIssue>()
            if (audioPerfect?.isChecked == true) audioIssues.add(AudioIssue.CALL_PERFECT)
            if (audioIssueDropped?.isChecked == true) audioIssues.add(AudioIssue.CALL_DROPPED)
            if (audioIssueHearOther?.isChecked == true) audioIssues.add(AudioIssue.COULD_NOT_HEAR_OTHER)
            if (audioIssueHearMe?.isChecked == true) audioIssues.add(AudioIssue.OTHER_COULD_NOT_HEAR_ME)
            if (audioIssueBackgroundNoise?.isChecked == true) audioIssues.add(AudioIssue.BACKGROUND_NOISE)
            if (audioIssueEcho?.isChecked == true) audioIssues.add(AudioIssue.ECHO)

            val environment = when {
                envIndoor?.isChecked == true -> Environment.INDOOR
                envOutdoor?.isChecked == true -> Environment.OUTDOOR
                envVehicle?.isChecked == true -> Environment.IN_VEHICLE
                envNoisyArea?.isChecked == true -> Environment.NOISY_AREA
                else -> null
            }

            val comment = commentInput?.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            val feedback = UserFeedback(
                voiceQuality = voiceQuality,
                audioIssues = audioIssues.takeIf { it.isNotEmpty() },
                environment = environment,
                comment = comment
            )

            onFeedbackResult?.invoke(feedback)
            submitBtn.isEnabled = false
            removeOverlay(context)
        }

        val params = WindowManager.LayoutParams().apply {
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
