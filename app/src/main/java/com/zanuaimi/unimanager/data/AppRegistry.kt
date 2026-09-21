package com.zanuaimi.unimanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppRegistry(context: Context) {
    companion object {
        const val PROTOCOL_VERSION = 1
        const val CAPABILITY_OVERLAY = "overlay.config.v2"
        const val CAPABILITY_BLOCK_ADS = "block_ads.v1"
        const val CAPABILITY_BLOCK_HOSTS = "block_ads_hosts.v1"
        private val SUPPORTED_CAPABILITIES = setOf(
            CAPABILITY_OVERLAY,
            CAPABILITY_BLOCK_ADS,
            CAPABILITY_BLOCK_HOSTS,
        )
    }

    private val preferences = context.getSharedPreferences("registered_apps", Context.MODE_PRIVATE)

    @Synchronized
    fun register(payload: String): String {
        val incoming = parse(payload) ?: return "{}"
        val packageName = canonicalPackageName(incoming.optString("package_name"))
        if (packageName.isBlank()) return "{}"
        incoming.put("package_name", packageName)
        val current = parse(preferences.getString(packageName, null)) ?: JSONObject()
        val merged = JSONObject(current.toString())
        val keys = incoming.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "configuration" && incoming.optJSONObject(key) != null) {
                val configuration = JSONObject(merged.optJSONObject(key)?.toString() ?: "{}")
                mergeMissing(configuration, incoming.optJSONObject(key)!!)
                merged.put(key, configuration)
            } else {
                merged.put(key, incoming.get(key))
            }
        }
        merged.put("manager_last_seen_at", System.currentTimeMillis())
        // Registration is a patch-install/update event. Commit it before replying so
        // a manager or host process crash cannot acknowledge an unpersisted record.
        if (!preferences.edit().putString(packageName, merged.toString()).commit()) {
            return "{\"status\":\"registration_failed\"}"
        }
        return configuration(merged)
    }

    @Synchronized
    fun read(payload: String): String {
        val packageName = canonicalPackageName(parse(payload)?.optString("package_name").orEmpty())
        return configuration(parse(preferences.getString(packageName, null)) ?: JSONObject())
    }

    @Synchronized
    fun update(payload: String): String {
        val incoming = parse(payload) ?: return "{}"
        val packageName = canonicalPackageName(incoming.optString("package_name"))
        if (packageName.isBlank()) return "{}"
        val current = parse(preferences.getString(packageName, null)) ?: return "{}"
        val incomingConfiguration = incoming.optJSONObject("configuration") ?: return configuration(current)
        val merged = JSONObject(current.toString())
        val configuration = JSONObject(merged.optJSONObject("configuration")?.toString() ?: "{}")
        // Runtime updates are authoritative for the keys they provide. Patch registration uses
        // mergeMissing separately so a repatch does not erase manager-owned values.
        merge(configuration, incomingConfiguration)
        merged.put("configuration", configuration)
        merged.put("manager_last_updated_at", System.currentTimeMillis())
        if (!preferences.edit().putString(packageName, merged.toString()).commit()) return "{}"
        return configuration(merged)
    }

    /** Updates only manager-owned configuration keys and keeps unknown patch data intact. */
    @Synchronized
    fun updateConfiguration(packageName: String, values: JSONObject): Boolean {
        val canonicalName = canonicalPackageName(packageName)
        if (canonicalName.isBlank()) return false
        val current = parse(preferences.getString(canonicalName, null)) ?: return false
        val configuration = JSONObject(current.optJSONObject("configuration")?.toString() ?: "{}")
        merge(configuration, values)
        current.put("configuration", configuration)
        current.put("manager_last_updated_at", System.currentTimeMillis())
        return preferences.edit().putString(canonicalName, current.toString()).commit()
    }

    @Synchronized
    fun remove(packageName: String): Boolean {
        val canonicalName = canonicalPackageName(packageName)
        if (canonicalName.isBlank()) return false
        return preferences.edit().remove(canonicalName).commit()
    }

    fun get(packageName: String): JSONObject? {
        val canonicalName = canonicalPackageName(packageName)
        return parse(preferences.getString(canonicalName, null))?.let { JSONObject(it.toString()) }
    }

    fun configuration(packageName: String): JSONObject =
        get(packageName)?.optJSONObject("configuration")?.let { JSONObject(it.toString()) } ?: JSONObject()

    @Synchronized
    fun all(): List<JSONObject> {
        val uniqueApps = linkedMapOf<String, JSONObject>()
        preferences.all.values.mapNotNull { value -> parse(value as? String) }.forEach { app ->
            val packageName = canonicalPackageName(app.optString("package_name"))
            if (packageName.isBlank()) return@forEach
            app.put("package_name", packageName)
            // Package name is the identity. If older data contains duplicates,
            // keep the most recently registered record for that package.
            val previous = uniqueApps[packageName]
            if (previous == null || app.optLong("manager_last_seen_at") >= previous.optLong("manager_last_seen_at")) {
                uniqueApps[packageName] = app
            }
        }
        return uniqueApps.values.sortedBy { it.optString("app_label").lowercase() }
    }

    fun hasCapability(app: JSONObject, capability: String): Boolean {
        val capabilities = app.optJSONArray("capabilities") ?: return false
        for (index in 0 until capabilities.length()) {
            if (capability == capabilities.optString(index)) return true
        }
        return false
    }

    fun status(app: JSONObject): RegistrationStatus {
        val status = app.optString("status").trim().lowercase()
        val patchStatus = app.optString("patch_status").trim().lowercase()
        val compatibility = app.optString("compatibility").trim().lowercase()
        val explicitUnsupported = app.optBoolean("unsupported", false) ||
            status in setOf("unsupported", "incompatible", "failed") ||
            patchStatus in setOf("unsupported", "incompatible", "failed") ||
            compatibility in setOf("unsupported", "incompatible")
        if (explicitUnsupported) {
            return RegistrationStatus(
                label = "Unsupported",
                detail = app.optString("unsupported_reason").ifBlank { "This patched app cannot use the manager capabilities reported by the registration." },
                kind = StatusKind.UNSUPPORTED,
            )
        }

        val explicitRepatch = app.optBoolean("repatch_required", false) ||
            app.optBoolean("needs_repatch", false) ||
            status in setOf("repatch_required", "repatch-required", "stale") ||
            patchStatus in setOf("repatch_required", "repatch-required", "stale")
        if (explicitRepatch) {
            return RegistrationStatus(
                label = "Repatch required",
                detail = app.optString("repatch_reason").ifBlank { "The installed patch is out of date for the reported manager data." },
                kind = StatusKind.REPATCH_REQUIRED,
            )
        }

        val protocol = app.optInt("protocol_version", PROTOCOL_VERSION)
        if (protocol > PROTOCOL_VERSION) {
            return RegistrationStatus(
                label = "Unsupported",
                detail = "This app requires bridge protocol v$protocol; this manager supports v$PROTOCOL_VERSION.",
                kind = StatusKind.UNSUPPORTED,
            )
        }
        if (protocol in 1 until PROTOCOL_VERSION) {
            return RegistrationStatus(
                label = "Repatch required",
                detail = "The app uses an older bridge protocol. Repatch it before changing manager settings.",
                kind = StatusKind.REPATCH_REQUIRED,
            )
        }
        val unsupported = unsupportedCapabilities(app)
        if (unsupported.isNotEmpty()) {
            return RegistrationStatus(
                label = "Unsupported capability",
                detail = "This manager does not support: ${unsupported.joinToString()}",
                kind = StatusKind.UNSUPPORTED,
            )
        }
        return RegistrationStatus("Registered", "Registration and capabilities are available.", StatusKind.READY)
    }

    fun unsupportedCapabilities(app: JSONObject): List<String> {
        val capabilities = app.optJSONArray("capabilities") ?: return emptyList()
        return (0 until capabilities.length())
            .mapNotNull { capabilities.optString(it).takeIf(String::isNotBlank) }
            .filterNot(SUPPORTED_CAPABILITIES::contains)
    }

    private fun merge(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            target.put(key, source.get(key))
        }
    }

    private fun mergeMissing(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!target.has(key)) target.put(key, source.get(key))
        }
    }

    private fun configuration(app: JSONObject): String {
        val result = JSONObject()
        val config = app.optJSONObject("configuration") ?: return result.toString()
        val keys = config.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result.put(key, config.get(key))
        }
        return result.toString()
    }

    private fun parse(value: String?): JSONObject? = runCatching {
        value?.takeIf { it.isNotBlank() }?.let(::JSONObject)
    }.getOrNull()

    private fun canonicalPackageName(value: String): String = value.trim()

    enum class StatusKind { READY, UNSUPPORTED, REPATCH_REQUIRED }

    data class RegistrationStatus(
        val label: String,
        val detail: String,
        val kind: StatusKind,
    )
}
