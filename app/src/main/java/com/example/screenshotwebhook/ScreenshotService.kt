package com.example.screenshotwebhook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenshotService : Service() {

    companion object {
        const val CHANNEL_ID = "screenshot_svc"
        const val NOTIF_ID = 1001
        const val ACTION_SEND = "com.example.screenshotwebhook.ACTION_SEND"

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotService::class.java)
            context.startForegroundService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sendJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND) {
            triggerSend()
        }
        return START_STICKY
    }

    private fun triggerSend() {
        if (sendJob?.isActive == true) {
            Toast.makeText(this, "Already sending…", Toast.LENGTH_SHORT).show()
            return
        }
        sendJob = scope.launch {
            val result = ScreenshotSender.send(this@ScreenshotService)
            val msg = result.getOrElse { "Failed: ${it.message}" }
            Toast.makeText(this@ScreenshotService, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sendIntent = Intent(this, ScreenshotService::class.java).apply { action = ACTION_SEND }
        val sendPending = PendingIntent.getService(
            this, 1, sendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screenshot Webhook")
            .setContentText("Tap Send to post the latest screenshot")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Send Screenshot",
                sendPending
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "Screenshot Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps the app alive for quick screenshot sending" }
        mgr.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
