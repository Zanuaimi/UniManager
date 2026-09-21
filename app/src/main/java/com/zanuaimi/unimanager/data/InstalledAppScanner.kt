package com.zanuaimi.unimanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Imports UniPatches registrations embedded in installed APK manifests. */
object InstalledAppScanner {
    private const val METADATA_NAME = "com.zanuaimi.unimanager.REGISTRATION"
    private const val CACHE_NAME = "installed_app_scan"
    private const val LAST_SCAN_KEY = "last_scan_at"
    const val SETTINGS_NAME = "uni_manager_settings"
    const val KEY_ENABLE_PULL_REFRESH = "enable_pull_refresh"
    const val KEY_ENABLE_AUTO_REFRESH = "enable_auto_refresh"
    const val KEY_AUTO_REFRESH_VALUE = "auto_refresh_value"
    const val KEY_AUTO_REFRESH_UNIT = "auto_refresh_unit"
    const val MIN_REFRESH_COOLDOWN_SECONDS = 30L

    /** Avoids repeating the package-manager scan during rapid Activity resumes. */
    fun scanIfNeeded(context: Context): Int {
        val preferences = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
        val settings = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean(KEY_ENABLE_AUTO_REFRESH, true)) return 0
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - preferences.getLong(LAST_SCAN_KEY, 0L)) / 1000L
        if (elapsedSeconds < cooldownSeconds(settings)) return 0
        val imported = scan(context, preferences)
        preferences.edit().putLong(LAST_SCAN_KEY, now).commit()
        return imported
    }

    /** Forces a scan, useful after a user removes a registry entry. */
    fun invalidate(context: Context) {
        context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
            .edit().remove(LAST_SCAN_KEY).apply()
    }

    fun scan(context: Context): Int {
        val preferences = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
        return scan(context, preferences)
    }

    private fun scan(context: Context, preferences: android.content.SharedPreferences): Int {
        val packageManager = context.packageManager
        val registry = AppRegistry(context)
        val applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var imported = 0
        applications.forEach { application ->
            val encoded = application.metaData?.getString(METADATA_NAME).orEmpty()
            if (encoded.isBlank()) return@forEach
            val packageName = application.packageName
            val fingerprint = fingerprint(application, encoded)
            val cachedFingerprint = preferences.getString("fingerprint_$packageName", null)
            if (cachedFingerprint == fingerprint && registry.get(packageName) != null) return@forEach
            val registration = decode(encoded) ?: return@forEach
            registration.put("package_name", packageName)
            registration.put(
                "app_label",
                registration.optString("app_label").ifBlank {
                    application.loadLabel(packageManager).toString()
                },
            )
            registration.put(
                "version_name",
                registration.optString("version_name").ifBlank {
                    runCatching { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }.getOrDefault("")
                },
            )
            val result = runCatching { JSONObject(registry.register(registration.toString())) }.getOrNull()
            if (result?.optString("status") != "registration_failed") {
                preferences.edit().putString("fingerprint_$packageName", fingerprint).apply()
                imported++
            }
        }
        return imported
    }

    private fun cooldownSeconds(settings: android.content.SharedPreferences): Long {
        val value = settings.getLong(KEY_AUTO_REFRESH_VALUE, 10L).coerceAtLeast(MIN_REFRESH_COOLDOWN_SECONDS)
        val multiplier = when (settings.getString(KEY_AUTO_REFRESH_UNIT, "m")) {
            "s" -> 1L
            "h" -> 60L * 60L
            "d" -> 24L * 60L * 60L
            else -> 60L
        }
        return if (value > Long.MAX_VALUE / multiplier) Long.MAX_VALUE else value * multiplier
    }

    private fun fingerprint(application: ApplicationInfo, encoded: String): String {
        val sourceTimestamp = runCatching { File(application.sourceDir).lastModified() }.getOrDefault(0L)
        val input = "${application.packageName}|$sourceTimestamp|$encoded"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun decode(encoded: String): JSONObject? = runCatching {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        JSONObject(String(bytes, Charsets.UTF_8)).takeIf {
            it.optString("format") == "unipatches-unimanager-registration-v1"
        }
    }.getOrNull()
}
