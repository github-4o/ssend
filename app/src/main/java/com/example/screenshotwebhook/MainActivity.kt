package com.example.screenshotwebhook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notifPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Start the permanent foreground service
        ScreenshotService.start(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Screenshot Sender", style = MaterialTheme.typography.headlineMedium)

        Text(
            "A permanent notification is active in your tray. Tap the \"Send Screenshot\" button in the notification to POST the latest image to:\n\nhttp://192.168.0.203:80085",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                sending = true
                scope.launch {
                    val result = ScreenshotSender.send(context)
                    sending = false
                    Toast.makeText(
                        context,
                        result.getOrElse { "Failed: ${it.message}" },
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            enabled = !sending,
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text(if (sending) "Sending…" else "Test Send Now") 
        }
    }
}
