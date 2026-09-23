package com.zanuaimi.unimanager.data.repository

import android.content.Context
import com.zanuaimi.unimanager.data.AppRegistry
import com.zanuaimi.unimanager.data.model.RegisteredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppRegistryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val registry = AppRegistry(appContext)

    suspend fun getAll(): List<RegisteredApp> = withContext(Dispatchers.IO) {
        registry.all().map(::toModel)
    }

    suspend fun get(packageName: String): RegisteredApp? = withContext(Dispatchers.IO) {
        registry.get(packageName)?.let(::toModel)
    }

    suspend fun remove(packageName: String): Boolean = withContext(Dispatchers.IO) {
        registry.remove(packageName)
    }

    suspend fun updateConfiguration(packageName: String, values: JSONObject): Boolean =
        withContext(Dispatchers.IO) {
            registry.updateConfiguration(packageName, values)
        }

    suspend fun register(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        registry.register(payload.toString(), clearRemoval = true).let { response ->
            JSONObject(response).optString("status").isBlank()
        }
    }

    suspend fun configuration(packageName: String): JSONObject = withContext(Dispatchers.IO) {
        registry.configuration(packageName)
    }

    private fun toModel(app: JSONObject): RegisteredApp {
        val status = registry.status(app)
        return RegisteredApp(
            packageName = app.optString("package_name"),
            label = app.optString("app_label").ifBlank { "Unknown app" },
            version = listOf("app_version", "version_name", "version")
                .firstNotNullOfOrNull { app.optString(it).takeIf(String::isNotBlank) }
                ?: "Version unknown",
            raw = JSONObject(app.toString()),
            statusLabel = status.label,
            statusDetail = status.detail,
            isEditable = status.kind == AppRegistry.StatusKind.READY,
        )
    }
}
