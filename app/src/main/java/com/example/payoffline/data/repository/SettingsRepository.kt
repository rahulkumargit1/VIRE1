package com.example.payoffline.data.repository

import android.content.Context
import com.example.payoffline.data.model.AppSettings
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("payoffline_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val json = prefs.getString("settings", null) ?: return AppSettings()
        return try {
            gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
        } catch (_: Exception) { AppSettings() }
    }

    suspend fun updateSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        _settings.value = settings
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }

    suspend fun addRecipient(recipient: String) = withContext(Dispatchers.IO) {
        val current = _settings.value
        val updated = current.savedRecipients.toMutableList()
        if (!updated.contains(recipient)) {
            updated.add(0, recipient)
            if (updated.size > 20) updated.removeLastOrNull()
        }
        updateSettings(current.copy(savedRecipients = updated))
    }

    suspend fun removeRecipient(recipient: String) = withContext(Dispatchers.IO) {
        val current = _settings.value
        val updated = current.savedRecipients.toMutableList()
        updated.remove(recipient)
        updateSettings(current.copy(savedRecipients = updated))
    }
}
