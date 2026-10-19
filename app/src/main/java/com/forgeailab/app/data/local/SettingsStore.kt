package com.forgeailab.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("forge_settings")

class SettingsStore(private val context: Context) {
    companion object {
        private val KEY_API = stringPreferencesKey("gemini_api_key")
        private val KEY_DARK = booleanPreferencesKey("dark_theme")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API] ?: "" }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK] ?: true }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[KEY_API] = value.trim() }
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[KEY_DARK] = value }
    }
}
