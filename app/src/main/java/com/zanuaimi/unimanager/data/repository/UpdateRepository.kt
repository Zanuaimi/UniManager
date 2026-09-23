package com.zanuaimi.unimanager.data.repository

import android.content.Context
import com.zanuaimi.unimanager.BuildConfig
import com.zanuaimi.unimanager.data.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val latestUrl = "https://api.github.com/repos/Zanuaimi/UniManager/releases/latest"

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val connection = URL(latestUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "UniManager/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) return@withContext null
            val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val version = release.optString("tag_name").removePrefix("v")
            val apkUrl = findApkUrl(release, version)
            ReleaseInfo(version, apkUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun findApkUrl(release: JSONObject, version: String): String? {
        val assets = release.optJSONArray("assets") ?: return null
        val versionedName = "UniManager-$version.apk"
        var legacyUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url").takeIf { it.isNotBlank() } ?: continue
            if (name == versionedName) return url
            if (name == "UniManager.apk") legacyUrl = url
        }
        return legacyUrl
    }

    suspend fun download(url: String): File = withContext(Dispatchers.IO) {
        val directory = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val temporary = File(directory, "UniManager.apk.download")
        val target = File(directory, "UniManager.apk")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) error("Update download failed")
            connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
            check(!target.exists() || target.delete()) { "Could not replace previous update" }
            check(temporary.renameTo(target)) { "Could not prepare downloaded APK" }
            target
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun compareVersions(first: String, second: String): Int {
        val a = first.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val b = second.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { index ->
            a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 }).takeIf { it != 0 }
        } ?: 0
    }
}
