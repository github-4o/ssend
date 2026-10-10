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
    val configFlow = remember { ServerPrefs.configFlow(context) }
    
    var currentConfig by remember { mutableStateOf(ServerPrefs.ServerConfig("", "")) }
    var ipInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configFlow.collect { config ->
            currentConfig = config
            if (ipInput.isBlank()) ipInput = config.ip
            if (portInput.isBlank()) portInput = config.port
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Screenshot Sender", style = MaterialTheme.typography.headlineMedium)

        Text(
            "Configure your local server. The permanent notification in your tray has a \"Send\" button to instantly POST the latest screenshot.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text("Server IP") },
            placeholder = { Text("192.168.0.203") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it },
            label = { Text("Server Port") },
            placeholder = { Text("80085") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        ServerPrefs.setConfig(context, ipInput, portInput)
                        Toast.makeText(context, "Configuration Saved", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }

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
                enabled = !sending && currentConfig.ip.isNotBlank() && currentConfig.port.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text(if (sending) "Sending…" else "Test Send") }
        }

        if (currentConfig.ip.isNotBlank() && currentConfig.port.isNotBlank()) {
            Text(
                "Active Target: http://${currentConfig.ip}:${currentConfig.port}", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text("No server configured yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
