package com.example.discordsender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Action) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val webhook = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("webhook", null)
            if (!webhook.isNullOrEmpty()) {
                val serviceIntent = Intent(context, NotificationService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}

