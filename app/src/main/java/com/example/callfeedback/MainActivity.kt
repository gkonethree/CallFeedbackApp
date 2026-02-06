package com.example.callfeedback

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

    private var overlayPermissionRequested = false

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
            startCallMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPhonePermission()

        setContent {
            CallFeedbackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
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
            startCallMonitorService()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startCallMonitorService() {
        val intent = Intent(this, CallMonitorService::class.java)
        startService(intent)

        try {
            if (!OverlayHelper.canDrawOverlays(this) && !overlayPermissionRequested) {
                overlayPermissionRequested=true
                val overlayIntent = OverlayHelper.requestOverlayPermissionIntent(this)
                startActivity(overlayIntent)
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Failed to request overlay permission", t)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CallFeedbackTheme {
        Greeting("Android")
    }
}
