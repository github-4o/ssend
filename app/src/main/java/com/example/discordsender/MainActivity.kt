package com.example.discordsender

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class NotificationService : Service() {
    private val CHANNEL_ID = "PermanentChannel"
    private val ACTION_SEND = "com.example.discordsender.SEND_IMAGE"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND) {
            sendLatestImage()
        }

        val sendIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_SEND }
        val pendingIntent = PendingIntent.getService(this, 0, sendIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Discord Image Sender")
            .setContentText("Tap to send the last photo taken.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_send, "Send Last Photo", pendingIntent)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun sendLatestImage() {
        thread {
            val webhookUrl = getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("webhook", null) ?: return@thread
            val latestImagePath = getLatestImagePath() ?: return@thread
            val file = File(latestImagePath)
            if (!file.exists()) return@thread

            try {
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = URL(webhookUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                DataOutputStream(conn.outputStream).use { output ->
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"files[0]\"; filename=\"${file.name}\"\r\n")
                    output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                    output.write(file.readBytes())
                    output.writeBytes("\r\n--$boundary--\r\n")
                    output.flush()
                }
                conn.responseCode // Triggers network call
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLatestImagePath(): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            }
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Permanent Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

