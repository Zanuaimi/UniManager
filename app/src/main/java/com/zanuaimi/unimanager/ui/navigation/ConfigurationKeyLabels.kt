package com.zanuaimi.unimanager.ui.navigation

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts the stable configuration keys exchanged with patches into labels
 * that are useful to people. Raw keys remain the storage and bridge contract.
 */
internal object ConfigurationKeyLabels {
    data class Choice(val label: String, val value: String)
    private data class Descriptor(val label: String, val type: String?, val choices: List<Choice>)

    private val universalOverlayChoices = mapOf(
        "runtimeOverlayStatisticMonitorPosition" to listOf(
            Choice("No stat monitors", "none"),
            Choice("Above overlay button", "top"),
            Choice("Below overlay button", "bottom"),
        ),
        "runtimeOverlayMonitorScale" to listOf("0.75" to "0.75x", "1" to "1x", "1.25" to "1.25x", "1.5" to "1.5x", "2" to "2x").map { Choice(it.second, it.first) },
        "runtimeOverlayMonitorColumns" to listOf("1" to "1 column", "2" to "2 columns", "3" to "3 columns").map { Choice(it.second, it.first) },
        "runtimeOverlayTemperatureFormat" to listOf("celsius" to "Celsius", "fahrenheit" to "Fahrenheit", "kelvin" to "Kelvin").map { Choice(it.second, it.first) },
        "runtimeOverlayTimeFormat" to listOf("12" to "12-hour clock", "24" to "24-hour clock").map { Choice(it.second, it.first) },
        "runtimeOverlayControlTheme" to listOf("legacy" to "Legacy", "modern" to "Modern", "monet" to "Monet-style").map { Choice(it.second, it.first) },
        "runtimeOverlayBottomButtonStyle" to listOf("text" to "Text only", "solid" to "Solid background", "gradient" to "Gradient background").map { Choice(it.second, it.first) },
        "runtimeOverlayBottomButtonShape" to listOf("square" to "Full square", "squircle" to "Squircle").map { Choice(it.second, it.first) },
        "runtimeOverlaySeparatorStyle" to listOf("ascii" to "Module type separators", "doubleLine" to "Lines above and below", "background" to "Background behind text", "singleLine" to "Line below", "inline" to "Inline after module name").map { Choice(it.second, it.first) },
        "runtimeOverlayTitleIconPlacement" to listOf("none" to "No icons", "left" to "Top left", "right" to "Top right", "both" to "Both sides").map { Choice(it.second, it.first) },
        "runtimeOverlayTitleAlignment" to listOf("left" to "Left", "center" to "Center", "right" to "Right").map { Choice(it.second, it.first) },
        "runtimeOverlayMenuCorners" to listOf("rounded" to "Rounded", "square" to "Square").map { Choice(it.second, it.first) },
        "runtimeOverlayMenuOutlineAnimation" to listOf("static" to "Static", "gradient" to "Horizontal scrolling gradient", "vertical" to "Vertical gradient", "rainbow" to "Rainbow gradient").map { Choice(it.second, it.first) },
        "runtimeOverlayOpeningAnimation" to listOf("fade" to "Fade", "appearRight" to "Appear from right", "appearTop" to "Appear from top", "appearBottom" to "Appear from bottom", "appearLeft" to "Appear from left", "scale" to "Scale", "disabled" to "Disabled").map { Choice(it.second, it.first) },
        "runtimeOverlayClosingAnimation" to listOf("fade" to "Fade", "disappearUp" to "Disappear upwards", "disappearDown" to "Disappear downwards", "disappearLeft" to "Disappear leftwards", "disappearRight" to "Disappear rightwards", "scale" to "Scale", "disabled" to "Disabled").map { Choice(it.second, it.first) },
        "runtimeOverlayAnimationEasing" to listOf("linear" to "Linear", "logarithmic" to "Logarithmic").map { Choice(it.second, it.first) },
        "runtimeOverlayDescriptionAlignment" to listOf("left" to "Left", "center" to "Center", "right" to "Right").map { Choice(it.second, it.first) },
        "runtimeOverlayIconTextFont" to fontChoices(),
        "runtimeOverlayMenuTextFont" to fontChoices(),
        "runtimeOverlayIconStyle" to listOf("parts" to "Multi-parts icon", "text" to "Text icon").map { Choice(it.second, it.first) },
        "runtimeOverlayIconBackgroundStyle" to listOf("flat" to "Flat", "faceted" to "Faceted layers").map { Choice(it.second, it.first) },
        "runtimeOverlayButtonShape" to listOf("circle" to "Circle", "squircle" to "Squircle", "square" to "Square").map { Choice(it.second, it.first) },
        "runtimeOverlayButtonPosition" to listOf(
            "topLeft" to "Top left", "topMiddle" to "Top middle", "topRight" to "Top right",
            "centerLeft" to "Center left", "centerRight" to "Center right",
            "bottomLeft" to "Bottom left", "bottomMiddle" to "Bottom middle", "bottomRight" to "Bottom right",
        ).map { Choice(it.second, it.first) },
    )

    private fun fontChoices() = listOf(
        "default" to "Default (Android)", "roboto" to "Roboto", "sansSerif" to "Sans serif",
        "serif" to "Serif", "monospace" to "Monospace", "sansCondensed" to "Sans condensed",
        "sansMedium" to "Sans medium", "sansBlack" to "Sans black",
    ).map { Choice(it.second, it.first) }

    fun choices(app: JSONObject?, key: String): List<Choice>? = descriptor(app, key)?.choices?.takeIf { it.isNotEmpty() } ?: universalOverlayChoices[key]

    fun presetChoices(app: JSONObject?): List<Choice> = buildList {
        add(Choice("Custom (UniPatches defaults)", "custom"))
        val catalog = app?.optJSONArray("preset_catalog")
        if (catalog != null && catalog.length() > 0) {
            for (index in 0 until catalog.length()) {
                val preset = catalog.optJSONObject(index) ?: continue
                val id = preset.optString("id")
                if (id.isNotBlank() && id != "custom") {
                    add(Choice(preset.optString("name").ifBlank { id }, id))
                }
            }
        } else {
            BuiltInOverlayPresets.definitions.forEach { add(Choice(it.name, it.id)) }
        }
    }

    fun presetConfiguration(app: JSONObject?, id: String): JSONObject? {
        app?.optJSONArray("preset_catalog")?.let { catalog ->
            for (index in 0 until catalog.length()) {
                val preset = catalog.optJSONObject(index) ?: continue
                if (preset.optString("id") == id) {
                    preset.optJSONObject("configuration")?.let { return JSONObject(it.toString()) }
                }
            }
        }
        return BuiltInOverlayPresets.definitions
            .firstOrNull { it.id == id }
            ?.configuration
            ?.let { JSONObject(it.toString()) }
    }

    fun presetVersion(app: JSONObject?, id: String): Int? {
        app?.optJSONArray("preset_catalog")?.let { catalog ->
            for (index in 0 until catalog.length()) {
                val preset = catalog.optJSONObject(index) ?: continue
                if (preset.optString("id") == id) {
                    preset.optInt("version").takeIf { it > 0 }?.let { return it }
                }
            }
        }
        return BuiltInOverlayPresets.definitions.firstOrNull { it.id == id }?.version
    }

    fun type(app: JSONObject?, key: String): String? = descriptor(app, key)?.type

    fun hierarchy(app: JSONObject?, key: String): List<String> =
        label(app, key).split(" > ").filter(String::isNotBlank)

    fun leafLabel(app: JSONObject?, key: String): String =
        hierarchy(app, key).lastOrNull() ?: title(key)

    fun label(app: JSONObject?, key: String): String {
        val normalizedKey = key.replaceFirst(Regex("^(RuntimeControls|runtimeControls)"), "runtimeOverlay")
        val patch = patchName(app, normalizedKey)
        val descriptor = descriptor(app, normalizedKey)
        if (descriptor != null && descriptor.label.isNotBlank()) return "$patch > ${descriptor.label}"
        val path = when {
            normalizedKey == "block_ads" -> "Block Ads > Enable"
            normalizedKey == "block_hosts" -> "Block Ads / Tracking Hosts > Enable"
            normalizedKey.startsWith("block_") -> "Block Ads > ${title(normalizedKey.removePrefix("block_"))}"
            normalizedKey == "runtimeOverlayEnableUniManagerIntegration" -> "Overlay integration > UniManager > Enable integration"
            normalizedKey == "runtimeOverlayRememberUniManagerRuntimeChanges" -> "Overlay integration > UniManager > Remember runtime changes"
            normalizedKey == "runtimeOverlaySelectedPreset" -> "UI preset"
            normalizedKey == "runtimeOverlayActivateStatisticsOnLaunch" -> "Module Settings > Statistic Modules Settings > Activate statistics on launch"
            normalizedKey == "runtimeOverlayEnableMonitorsOnLaunch" -> "Module Settings > Statistic Modules Settings > Enable monitors on launch"
            normalizedKey in setOf(
                "runtimeOverlayStatisticMonitorPosition",
                "runtimeOverlayMonitorScale",
                "runtimeOverlayMonitorColumns",
                "runtimeOverlayTemperatureFormat",
                "runtimeOverlayTimeFormat",
            ) -> "Module Settings > Statistic Modules Settings > ${title(normalizedKey.removePrefix("runtimeOverlay"))}"
            normalizedKey == "runtimeOverlayEnableOverlayRuntimeLogsOnLaunch" -> "Module Settings > Overlay Runtime Log Settings > Enable runtime log on launch"
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
        val patches = app?.optJSONArray("patches") ?: JSONArray()
        for (index in 0 until patches.length()) {
            val patch = patches.optJSONObject(index) ?: continue
            val id = patch.optString("id").orEmpty()
            val prefixes = patch.optJSONArray("configuration_prefixes")
            if (id.isNotBlank() && prefixes != null && (0 until prefixes.length()).any { normalizedKey.startsWith(prefixes.optString(it)) }) {
                return patchDisplayName(id)
            }
        }
        // Prefixes are the backward-compatible fallback for older metadata that
        // predates configuration_prefixes.
        if (normalizedKey.startsWith("block_")) return "Ads Block Patch"
        if (normalizedKey.startsWith("runtimeOverlay")) return "Universal Overlay Patch"
        for (index in 0 until patches.length()) {
            val id = patches.optJSONObject(index)?.optString("id").orEmpty()
            if (id.isNotBlank()) return patchDisplayName(id)
        }
        return "Patch settings"
    }

    private fun descriptor(app: JSONObject?, key: String): Descriptor? {
        val schema = app?.optJSONArray("configuration_schema") ?: return null
        for (index in 0 until schema.length()) {
            val item = schema.optJSONObject(index) ?: continue
            if (item.optString("key") != key) continue
            val choices = item.optJSONArray("choices")?.let { values ->
                (0 until values.length()).mapNotNull { choiceIndex ->
                    when (val choice = values.opt(choiceIndex)) {
                        is JSONObject -> Choice(choice.optString("label"), choice.optString("value"))
                        is String -> Choice(choice, choice)
                        else -> null
                    }?.takeIf { it.label.isNotBlank() && it.value.isNotBlank() }
                }
            }.orEmpty()
            return Descriptor(item.optString("label"), item.optString("type").takeIf(String::isNotBlank), choices)
        }
        return null
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
