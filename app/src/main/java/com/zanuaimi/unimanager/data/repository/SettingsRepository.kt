package com.zanuaimi.unimanager.data.repository

import android.content.Context
import com.zanuaimi.unimanager.data.InstalledAppScanner
import com.zanuaimi.unimanager.data.model.RefreshSettings
import com.zanuaimi.unimanager.data.model.AppearanceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        InstalledAppScanner.SETTINGS_NAME,
        Context.MODE_PRIVATE,
    )

    suspend fun read(): RefreshSettings = withContext(Dispatchers.IO) {
        RefreshSettings(
            pullToRefresh = preferences.getBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, true),
            automaticRefresh = preferences.getBoolean(InstalledAppScanner.KEY_ENABLE_AUTO_REFRESH, true),
            cooldownValue = preferences.getLong(InstalledAppScanner.KEY_AUTO_REFRESH_VALUE, 10L),
            cooldownUnit = preferences.getString(InstalledAppScanner.KEY_AUTO_REFRESH_UNIT, "m") ?: "m",
        )
    }

    suspend fun save(settings: RefreshSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, settings.pullToRefresh)
            .putBoolean(InstalledAppScanner.KEY_ENABLE_AUTO_REFRESH, settings.automaticRefresh)
            .putLong(InstalledAppScanner.KEY_AUTO_REFRESH_VALUE, settings.cooldownValue)
            .putString(InstalledAppScanner.KEY_AUTO_REFRESH_UNIT, settings.cooldownUnit)
            .apply()
    }

    suspend fun readAppearance(): AppearanceSettings = withContext(Dispatchers.IO) {
        AppearanceSettings(
            colorSet = preferences.getString(InstalledAppScanner.KEY_COLOR_SET, "dynamic") ?: "dynamic",
            customAccent = preferences.getString(InstalledAppScanner.KEY_CUSTOM_ACCENT, "#FF5656") ?: "#FF5656",
        )
    }

    suspend fun saveAppearance(settings: AppearanceSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(InstalledAppScanner.KEY_COLOR_SET, settings.colorSet)
            .putString(InstalledAppScanner.KEY_CUSTOM_ACCENT, settings.customAccent)
            .apply()
    }
}
