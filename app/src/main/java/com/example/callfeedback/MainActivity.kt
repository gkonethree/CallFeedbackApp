package com.example.callfeedback

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.callfeedback.service.CallMonitorService
import com.example.callfeedback.ui.overlay.OverlayHelper
import com.example.callfeedback.ui.theme.CallFeedbackTheme
import android.content.pm.PackageManager
import android.util.Log

class MainActivity : ComponentActivity() {

    private var permissionsGranted = false
    private var overlayHandled = false
    private var serviceStarted = false

    private val requestPhonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                ensureNotificationPermissionAndStartService()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "POST_NOTIFICATIONS granted")
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS denied")
            }
            requestLocationPermissions()
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "ACCESS_FINE_LOCATION granted")
            } else {
                Log.d("MainActivity", "ACCESS_FINE_LOCATION denied - location won't be collected")
            }
            permissionsGranted = true
            handleOverlayPermission()
        }

    private fun handleOverlayPermission() {
        try {
            if (!OverlayHelper.canDrawOverlays(this)) {
                val overlayIntent = OverlayHelper.requestOverlayPermissionIntent(this)
                startActivity(overlayIntent)  // user goes to settings
            } else {
                overlayHandled = true
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Overlay permission error", t)
            overlayHandled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPhonePermission()

        setContent {
            CallFeedbackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!overlayHandled && OverlayHelper.canDrawOverlays(this)) {
            overlayHandled = true
        }

        if (permissionsGranted && overlayHandled && !serviceStarted) {
            serviceStarted = true

            val intent = Intent(this, CallMonitorService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }
    private fun requestPhonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ensureNotificationPermissionAndStartService()
        } else {
            requestPhonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun ensureNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionsGranted = true
            handleOverlayPermission()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Setup complete \n\nYou can close the app.\nCall feedback will be recorded automatically.",
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CallFeedbackTheme {
        Text(text="This app helps you give feedback for your calls", modifier = Modifier)
    }
}
