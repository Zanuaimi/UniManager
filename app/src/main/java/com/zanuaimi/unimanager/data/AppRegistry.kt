package com.zanuaimi.unimanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

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
        const val MAX_PAYLOAD_BYTES = 512 * 1024
    }

    private val preferences = context.getSharedPreferences("registered_apps", Context.MODE_PRIVATE)
    private val removalPreferences = context.getSharedPreferences("removed_app_entries", Context.MODE_PRIVATE)

    @Synchronized
    fun register(payload: String, clearRemoval: Boolean = false): String {
        if (payload.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) return "{\"status\":\"payload_too_large\"}"
        val incoming = parse(payload) ?: return "{}"
        val packageName = canonicalPackageName(incoming.optString("package_name"))
        if (packageName.isBlank()) return "{}"
        incoming.put("package_name", packageName)
        if (clearRemoval) removalPreferences.edit().remove(packageName).commit()
        val current = parse(preferences.getString(packageName, null)) ?: JSONObject()
        val merged = JSONObject(current.toString())
        val keys = incoming.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "configuration" && incoming.optJSONObject(key) != null) {
                val configuration = JSONObject(merged.optJSONObject(key)?.toString() ?: "{}")
                pruneRepatchedKeys(configuration, incoming)
                mergeMissing(configuration, incoming.optJSONObject(key)!!)
                merged.put(key, configuration)
            } else {
                merged.put(key, incoming.get(key))
            }
        }
        syncPresetMetadata(merged, merged.optJSONObject("configuration"))
        merged.put("manager_last_seen_at", System.currentTimeMillis())
        // Registration is a patch-install/update event. Commit it before replying so
        // a manager or host process crash cannot acknowledge an unpersisted record.
        val serialized = merged.toString()
        if (serialized.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) return "{\"status\":\"payload_too_large\"}"
        if (!preferences.edit().putString(packageName, serialized).commit()) {
            return "{\"status\":\"registration_failed\"}"
        }
        return configuration(merged)
    }

    @Synchronized
    fun read(payload: String): String {
        val packageName = canonicalPackageName(parse(payload)?.optString("package_name").orEmpty())
        val stored = parse(preferences.getString(packageName, null)) ?: return "{}"
        // An app with a newer protocol or unsupported capability must use its embedded
        // defaults. Returning manager values here would make an incompatible bridge look valid.
        if (status(stored).kind != StatusKind.READY) return "{}"
        return configuration(stored)
    }

    @Synchronized
    fun update(payload: String): String {
        if (payload.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) return "{\"status\":\"payload_too_large\"}"
        val incoming = parse(payload) ?: return "{}"
        val packageName = canonicalPackageName(incoming.optString("package_name"))
        if (packageName.isBlank()) return "{}"
        val current = parse(preferences.getString(packageName, null)) ?: return "{}"
        if (status(current).kind != StatusKind.READY) return "{}"
        val incomingConfiguration = incoming.optJSONObject("configuration") ?: return configuration(current)
        val merged = JSONObject(current.toString())
        val configuration = JSONObject(merged.optJSONObject("configuration")?.toString() ?: "{}")
        // Runtime updates are authoritative for the keys they provide. Patch registration uses
        // mergeMissing separately so a repatch does not erase manager-owned values.
        merge(configuration, incomingConfiguration)
        merged.put("configuration", configuration)
        merged.put("manager_last_updated_at", System.currentTimeMillis())
        val serialized = merged.toString()
        if (serialized.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) return "{\"status\":\"payload_too_large\"}"
        if (!preferences.edit().putString(packageName, serialized).commit()) return "{}"
        return configuration(merged)
    }

    /** Updates only manager-owned configuration keys and keeps unknown patch data intact. */
    @Synchronized
    fun updateConfiguration(packageName: String, values: JSONObject): Boolean {
        val canonicalName = canonicalPackageName(packageName)
        if (canonicalName.isBlank()) return false
        val current = parse(preferences.getString(canonicalName, null)) ?: return false
        if (status(current).kind != StatusKind.READY) return false
        val configuration = JSONObject(current.optJSONObject("configuration")?.toString() ?: "{}")
        merge(configuration, values)
        current.put("configuration", configuration)
        syncPresetMetadata(current, configuration)
        current.put("manager_last_updated_at", System.currentTimeMillis())
        val serialized = current.toString()
        if (serialized.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) return false
        return preferences.edit().putString(canonicalName, serialized).commit()
    }

    @Synchronized
    fun remove(packageName: String): Boolean {
        val canonicalName = canonicalPackageName(packageName)
        if (canonicalName.isBlank()) return false
        val fingerprint = parse(preferences.getString(canonicalName, null))
            ?.optString("manager_metadata_fingerprint")
            .orEmpty()
        val removed = preferences.edit().remove(canonicalName).commit()
        if (!removed) return false
        return removalPreferences.edit().putString(canonicalName, fingerprint).commit()
    }

    fun isRemovalTombstoneCurrent(packageName: String, fingerprint: String): Boolean {
        val canonicalName = canonicalPackageName(packageName)
        return canonicalName.isNotBlank() && removalPreferences.getString(canonicalName, null) == fingerprint
    }

    fun clearRemoval(packageName: String) {
        val canonicalName = canonicalPackageName(packageName)
        if (canonicalName.isNotBlank()) removalPreferences.edit().remove(canonicalName).apply()
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

    /** Removes settings for modules that were removed during a later patch operation. */
    private fun pruneRepatchedKeys(configuration: JSONObject, incoming: JSONObject) {
        val patchIds = incoming.optJSONArray("patches") ?: return
        val prefixes = mutableListOf<String>()
        for (index in 0 until patchIds.length()) {
            when (patchIds.optJSONObject(index)?.optString("id")) {
                "universal-overlay" -> prefixes += "runtimeOverlay"
                "control-app-ads", "ads-block" -> prefixes += "block_"
            }
        }
        if (prefixes.isEmpty()) return
        val incomingConfiguration = incoming.optJSONObject("configuration") ?: JSONObject()
        val keys = configuration.keys().asSequence().toList()
        keys.filter { key -> prefixes.any { key.startsWith(it) } && !incomingConfiguration.has(key) }
            .forEach(configuration::remove)
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

    private fun syncPresetMetadata(target: JSONObject, configuration: JSONObject?) {
        if (configuration == null) return
        if (configuration.has("runtimeOverlaySelectedPreset")) {
            target.put("runtimeOverlaySelectedPreset", configuration.optString("runtimeOverlaySelectedPreset"))
        }
        if (configuration.has("runtimeOverlaySelectedPresetVersion")) {
            target.put("runtimeOverlaySelectedPresetVersion", configuration.optInt("runtimeOverlaySelectedPresetVersion"))
        }
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
