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
    val urlFlow = remember { WebhookPrefs.urlFlow(context) }
    var currentUrl by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        urlFlow.collect { url ->
            currentUrl = url
            if (input.isBlank()) input = url
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Screenshot Webhook", style = MaterialTheme.typography.headlineMedium)

        Text(
            "The permanent notification stays active. Tap its \"Send Screenshot\" button to POST the latest image from your Screenshots folder to the webhook below.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Webhook URL") },
            placeholder = { Text("https://example.com/webhook") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    WebhookPrefs.setUrl(context, input)
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Save") }

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
                enabled = !sending && currentUrl.isNotBlank()
            ) { Text(if (sending) "Sending…" else "Test Send") }
        }

        if (currentUrl.isNotBlank()) {
            Text("Active webhook: $currentUrl", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("No webhook configured yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
