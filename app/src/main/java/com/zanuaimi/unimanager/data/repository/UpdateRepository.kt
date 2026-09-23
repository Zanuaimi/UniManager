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
    private companion object {
        const val RELEASE_CONNECT_TIMEOUT_MS = 5_000
        const val RELEASE_READ_TIMEOUT_MS = 5_000
    }

    private val appContext = context.applicationContext
    private val latestUrl = "https://api.github.com/repos/Zanuaimi/UniManager/releases/latest"
    private val latestPageUrl = "https://github.com/Zanuaimi/UniManager/releases/latest"

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        fetchFromApi() ?: fetchFromReleasePage()
    }

    private fun fetchFromApi(): ReleaseInfo? {
        val connection = URL(latestUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = RELEASE_CONNECT_TIMEOUT_MS
            connection.readTimeout = RELEASE_READ_TIMEOUT_MS
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "UniManager/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) return null
            val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val version = normalizeVersion(release.optString("tag_name")) ?: return null
            val apkUrl = findApkUrl(release, version)
            return ReleaseInfo(version, apkUrl)
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    /** Uses GitHub's normal release redirect when the API is rate-limited or unavailable. */
    private fun fetchFromReleasePage(): ReleaseInfo? {
        val connection = URL(latestPageUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = RELEASE_CONNECT_TIMEOUT_MS
            connection.readTimeout = RELEASE_READ_TIMEOUT_MS
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "UniManager/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) return null
            val tag = connection.url.path.substringAfterLast('/').takeIf { it.isNotBlank() }
            val version = normalizeVersion(tag) ?: return null
            return ReleaseInfo(version, releaseAssetUrl(version))
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun findApkUrl(release: JSONObject, version: String): String? {
        val assets = release.optJSONArray("assets") ?: return null
        val versionedNames = setOf("UniManager-$version.apk", "UniManager-v$version.apk")
        var legacyUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url").takeIf { it.isNotBlank() } ?: continue
            if (name in versionedNames) return url
            if (name == "UniManager.apk") legacyUrl = url
        }
        return legacyUrl ?: releaseAssetUrl(version)
    }

    private fun releaseAssetUrl(version: String): String =
        "https://github.com/Zanuaimi/UniManager/releases/download/v$version/UniManager-$version.apk"

    private fun normalizeVersion(raw: String?): String? {
        val version = raw.orEmpty().trim().removePrefix("v")
        return version.takeIf { it.matches(Regex("\\d+(?:\\.\\d+){1,3}(?:[-+][0-9A-Za-z.-]+)?")) }
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
        val a = first.trim().removePrefix("v").split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val b = second.trim().removePrefix("v").split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { index ->
            a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 }).takeIf { it != 0 }
        } ?: 0
    }
}
