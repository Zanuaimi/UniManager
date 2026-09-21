package com.zanuaimi.unimanager.data.model

import android.content.pm.ApplicationInfo
import org.json.JSONObject

data class RegisteredApp(
    val packageName: String,
    val label: String,
    val version: String,
    val raw: JSONObject,
)

data class InstalledApp(
    val info: ApplicationInfo,
    val label: String,
    val version: String,
    val isSystem: Boolean,
)

enum class AppVisibility { USER, SYSTEM, ALL }

data class RefreshSettings(
    val pullToRefresh: Boolean = true,
    val automaticRefresh: Boolean = true,
    val cooldownValue: Long = 10,
    val cooldownUnit: String = "m",
) {
    fun rawCooldownSeconds(): Long {
        val multiplier = when (cooldownUnit) {
            "s" -> 1L
            "h" -> 60L * 60L
            "d" -> 24L * 60L * 60L
            else -> 60L
        }
        return if (cooldownValue > Long.MAX_VALUE / multiplier) {
            Long.MAX_VALUE
        } else {
            cooldownValue * multiplier
        }
    }

    fun cooldownSeconds(): Long = rawCooldownSeconds().coerceAtLeast(MIN_COOLDOWN_SECONDS)

    companion object {
        const val MIN_COOLDOWN_SECONDS = 30L
    }
}

data class AppearanceSettings(
    val colorSet: String = "dynamic",
    val customAccent: String = "#FF5656",
)

sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Empty(val message: String) : ScreenState<Nothing>
    data class Error(val message: String) : ScreenState<Nothing>
}

data class ReleaseInfo(
    val version: String,
    val apkUrl: String?,
)
