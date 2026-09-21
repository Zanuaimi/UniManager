package com.zanuaimi.unimanager.data.repository

import android.content.Context
import android.os.Build
import android.util.TypedValue
import com.zanuaimi.unimanager.data.InstalledAppScanner
import com.zanuaimi.unimanager.data.model.RefreshSettings
import com.zanuaimi.unimanager.data.model.AppearanceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
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
        readStoredAppearance()
    }

    /**
     * Resolves the appearance for a fresh install once. If the OS does not expose
     * a usable dynamic palette, persist the UniPatches palette as the fallback.
     */
    suspend fun readInitialAppearance(): AppearanceSettings = withContext(Dispatchers.IO) {
        if (preferences.contains(InstalledAppScanner.KEY_COLOR_SET)) {
            return@withContext readStoredAppearance()
        }

        val appearance = if (hasDynamicColorData()) {
            AppearanceSettings()
        } else {
            AppearanceSettings(colorSet = "unipatches")
        }
        preferences.edit()
            .putString(InstalledAppScanner.KEY_COLOR_SET, appearance.colorSet)
            .putString(InstalledAppScanner.KEY_CUSTOM_ACCENT, appearance.customAccent)
            .apply()
        appearance
    }

    suspend fun saveAppearance(settings: AppearanceSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(InstalledAppScanner.KEY_COLOR_SET, settings.colorSet)
            .putString(InstalledAppScanner.KEY_CUSTOM_ACCENT, settings.customAccent)
            .apply()
    }

    private fun readStoredAppearance() = AppearanceSettings(
        colorSet = preferences.getString(InstalledAppScanner.KEY_COLOR_SET, "dynamic") ?: "dynamic",
        customAccent = preferences.getString(InstalledAppScanner.KEY_CUSTOM_ACCENT, "#FF5656") ?: "#FF5656",
    )

    fun hasDynamicColorData(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val dynamicColorIds = intArrayOf(
            android.R.color.system_accent1_500,
            android.R.color.system_accent2_500,
            android.R.color.system_neutral1_500,
        )
        return dynamicColorIds.all { resourceId ->
            runCatching {
                val value = TypedValue()
                appContext.resources.getValue(resourceId, value, true)
                value.type != TypedValue.TYPE_NULL
            }.getOrDefault(false)
        }
    }
}
