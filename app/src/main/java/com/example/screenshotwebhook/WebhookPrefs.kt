package com.example.screenshotwebhook

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object WebhookPrefs {
    private val URL_KEY = stringPreferencesKey("webhook_url")

    fun urlFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[URL_KEY] ?: "" }

    suspend fun setUrl(context: Context, url: String) {
        context.dataStore.edit { it[URL_KEY] = url.trim() }
    }
}
