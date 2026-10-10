package com.example.screenshotwebhook

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object ServerPrefs {
    private val IP_KEY = stringPreferencesKey("server_ip")
    private val PORT_KEY = stringPreferencesKey("server_port")

    data class ServerConfig(val ip: String, val port: String)

    fun configFlow(context: Context): Flow<ServerConfig> =
        context.dataStore.data.map { prefs ->
            ServerConfig(
                ip = prefs[IP_KEY] ?: "",
                port = prefs[PORT_KEY] ?: ""
            )
        }

    suspend fun setConfig(context: Context, ip: String, port: String) {
        context.dataStore.edit { prefs ->
            prefs[IP_KEY] = ip.trim()
            prefs[PORT_KEY] = port.trim()
        }
    }
}
