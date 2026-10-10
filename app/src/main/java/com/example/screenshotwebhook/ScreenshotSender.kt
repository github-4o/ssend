package com.example.screenshotwebhook

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object ScreenshotSender {
    private const val TAG = "ScreenshotSender"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun candidateDirs(): List<File> {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        return listOf(
            File(pictures, "Screenshots"),
            File(dcim, "Screenshots"),
            File(dcim, "Screenshot"),
            File(pictures, "Screenshot"),
            pictures,
            dcim
        )
    }

    fun findLatestScreenshot(): File? {
        return candidateDirs()
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir ->
                (dir.listFiles()?.filter {
                    it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
                } ?: emptyList())
            }
            .maxByOrNull { it.lastModified() }
    }

    suspend fun send(context: Context): Result<String> = withContext(Dispatchers.IO) {
        // Idiomatic way to get a single value from a Flow in a suspend function
        val url = WebhookPrefs.urlFlow(context).first()
        
        if (url.isBlank()) return@withContext Result.failure(IllegalStateException("Webhook URL not set"))

        val file = findLatestScreenshot()
            ?: return@withContext Result.failure(NoSuchElementException("No screenshot found"))

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/${file.extension}".toMediaType())
            )
            .addFormDataPart("filename", file.name)
            .addFormDataPart("timestamp", System.currentTimeMillis().toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Sent ${file.name} (${file.length()} bytes)")
                } else {
                    Result.failure(RuntimeException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Send failed", t)
            Result.failure(t)
        }
    }
}
