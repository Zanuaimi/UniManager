package com.zanuaimi.unimanager.ui.navigation

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts the stable configuration keys exchanged with patches into labels
 * that are useful to people. Raw keys remain the storage and bridge contract.
 */
internal object ConfigurationKeyLabels {
    fun label(app: JSONObject?, key: String): String {
        val normalizedKey = key.replaceFirst(Regex("^(RuntimeControls|runtimeControls)"), "runtimeOverlay")
        val patch = patchName(app, normalizedKey)
        val path = when {
            normalizedKey == "block_ads" -> "Block Ads > Enable"
            normalizedKey == "block_hosts" -> "Block Ads / Tracking Hosts > Enable"
            normalizedKey.startsWith("block_") -> "Block Ads > ${title(normalizedKey.removePrefix("block_"))}"
            normalizedKey == "runtimeOverlayEnableUniManagerIntegration" -> "Overlay integration > UniManager > Enable integration"
            normalizedKey == "runtimeOverlayRememberUniManagerRuntimeChanges" -> "Overlay integration > UniManager > Remember runtime changes"
            normalizedKey == "runtimeOverlaySelectedPreset" -> "Quick setup > UI preset"
            normalizedKey.startsWith("runtimeOverlayInclude") -> "Modules > ${title(normalizedKey.removePrefix("runtimeOverlayInclude"))}"
            normalizedKey.startsWith("runtimeOverlayBottomButton") -> "UI settings > Bottom buttons > ${title(normalizedKey.removePrefix("runtimeOverlayBottomButton"))}"
            normalizedKey.startsWith("runtimeOverlayMenuText") -> "UI settings > Colors > ${title(normalizedKey.removePrefix("runtimeOverlayMenu"))}"
            normalizedKey.startsWith("runtimeOverlayMenu") -> "UI settings > Menu > ${title(normalizedKey.removePrefix("runtimeOverlayMenu"))}"
            normalizedKey.startsWith("runtimeOverlayTitle") -> "UI settings > Menu title > ${title(normalizedKey.removePrefix("runtimeOverlay"))}"
            normalizedKey == "runtimeOverlayIconShadow" -> "UI settings > Floating button > Icon shadow > Enable"
            normalizedKey.startsWith("runtimeOverlayIconShadow") -> "UI settings > Floating button > Icon shadow > ${title(normalizedKey.removePrefix("runtimeOverlayIconShadow"))}"
            normalizedKey.startsWith("runtimeOverlayIcon") -> "UI settings > Floating button > ${title(normalizedKey.removePrefix("runtimeOverlayIcon"))}"
            normalizedKey.startsWith("runtimeOverlayButton") -> "UI settings > Floating button > ${title(normalizedKey.removePrefix("runtimeOverlayButton"))}"
            normalizedKey.startsWith("runtimeOverlayActivity") -> "Advanced > Activity injection > ${title(normalizedKey.removePrefix("runtimeOverlayActivity"))}"
            normalizedKey.startsWith("runtimeOverlayImport") || normalizedKey.startsWith("runtimeOverlayExport") -> "UI settings > Presets > ${title(normalizedKey.removePrefix("runtimeOverlay"))}"
            normalizedKey.startsWith("runtimeOverlay") -> "UI settings > ${title(normalizedKey.removePrefix("runtimeOverlay"))}"
            else -> title(key)
        }
        return "$patch > $path"
    }

    fun patchName(app: JSONObject?, key: String = ""): String {
        val normalizedKey = key.replaceFirst(Regex("^(RuntimeControls|runtimeControls)"), "runtimeOverlay")
        if (normalizedKey.startsWith("block_")) return "Ads Block Patch"
        if (normalizedKey.startsWith("runtimeOverlay")) return "Universal Overlay Patch"
        val patches = app?.optJSONArray("patches") ?: JSONArray()
        for (index in 0 until patches.length()) {
            val id = patches.optJSONObject(index)?.optString("id").orEmpty()
            if (id.isNotBlank()) return patchDisplayName(id)
        }
        return "Patch settings"
    }

    private fun patchDisplayName(id: String): String = when (id) {
        "control-app-ads", "ads-block" -> "Ads Block Patch"
        "universal-overlay" -> "Universal Overlay Patch"
        else -> "${title(id)} Patch"
    }

    private fun title(value: String): String {
        if (value.isBlank()) return "Setting"
        return value
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Za-z])([0-9])"), "$1 $2")
            .replace(Regex("([0-9])([A-Za-z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(' ')
            .joinToString(" ") { word ->
                when (word.lowercase()) {
                    "ui" -> "UI"
                    "url" -> "URL"
                    "json" -> "JSON"
                    "fps" -> "FPS"
                    "dnd" -> "DND"
                    "ads" -> "Ads"
                    else -> word.replaceFirstChar(Char::uppercase)
                }
            }
    }
}
