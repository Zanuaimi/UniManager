package com.zanuaimi.unimanager.ui.navigation

import org.json.JSONArray
import org.json.JSONObject

/**
 * Built-in Universal Overlay presets owned by UniManager.
 *
 * Patched APKs only need to carry the selected preset and flattened values. Keeping
 * this catalog in the manager prevents the manifest registration value from growing
 * with every preset definition.
 */
internal object BuiltInOverlayPresets {
    data class Definition(
        val id: String,
        val name: String,
        val version: Int = 1,
        val configuration: JSONObject,
    )

    private fun base(): JSONObject = JSONObject().apply {
        put("runtimeOverlayIconBold", true)
        put("runtimeOverlayIconTextFont", "default")
        put("runtimeOverlayMenuTextFont", "default")
        put("runtimeOverlayIconTextColor2", "#FF5656")
        put("runtimeOverlayIconTextGradient", false)
        put("runtimeOverlayIconTextGradientAngle", 90)
        put("runtimeOverlayIconGradientBackground", true)
        put("runtimeOverlayIconOutline", false)
        put("runtimeOverlayIconOutlineWidthDp", 3)
        put("runtimeOverlayIconOutlineColor2", "#FFFFFF")
        put("runtimeOverlayIconStyle", "text")
        put("runtimeOverlayIconShape", "triangle")
        put("runtimeOverlayIconShapeColor1", "#FFFFFF")
        put("runtimeOverlayIconShapeColor2", "#FFFFFF")
        put("runtimeOverlayIconShapeGradient", false)
        put("runtimeOverlayIconShapeGradientAngle", 0)
        put("runtimeOverlayIconShapeStrokeWidth", 3)
        put("runtimeOverlayIconShapeScale", 70)
        put("runtimeOverlayIconHighlight", false)
        put("runtimeOverlayIconShadow", false)
        put("runtimeOverlayIconOutlineGradient", false)
        put("runtimeOverlayIconOutlineGradientAngle", 0)
        put("runtimeOverlayIconBackgroundStyle", "flat")
        put("runtimeOverlayIconGradientAngle", 0)
        put("runtimeOverlayIconParts", JSONArray())
        put("runtimeOverlayButtonShape", "circle")
        put("runtimeOverlayButtonSizeDp", 56)
        put("runtimeOverlayButtonIdleOpacityPercent", 50)
        put("runtimeOverlayButtonDragVisibilityDurationSeconds", 2)
        put("runtimeOverlayButtonPosition", "topRight")
        put("runtimeOverlayIconTextSizeSp", 18)
        put("runtimeOverlayMenuWidthLimit", 90)
        put("runtimeOverlayMenuHeightLimit", 45)
        put("runtimeOverlayShowExtraPopupHeaders", true)
        put("runtimeOverlayControlTheme", "modern")
        put("runtimeOverlayControlBackground", "#300000")
        put("runtimeOverlayControlForeground", "#FF5656")
        put("runtimeOverlayBottomButtonStyle", "text")
        put("runtimeOverlayBottomButtonShape", "square")
        put("runtimeOverlayBottomButtonPadding", false)
        put("runtimeOverlayBottomButtonTextColor", "#FF5656")
        put("runtimeOverlayBottomButtonBackground1", "#500000")
        put("runtimeOverlayBottomButtonBackground2", "#AA0000")
        put("runtimeOverlayTitleIconPlacement", "none")
        put("runtimeOverlayTitleAlignment", "left")
        put("runtimeOverlayTitleSeparator", false)
        put("runtimeOverlayMenuCorners", "rounded")
        put("runtimeOverlayMenuOutlineAnimation", "static")
        put("runtimeOverlayOutlineAnimationSpeed", 1)
        put("runtimeOverlayOpeningAnimation", "fade")
        put("runtimeOverlayClosingAnimation", "fade")
        put("runtimeOverlayAnimationDuration", 180)
        put("runtimeOverlayAnimationEasing", "linear")
    }

    private fun definition(
        id: String,
        name: String,
        background: String,
        transparency: Int,
        outline: String,
        textColor: String,
        buttonTextColor: String,
        buttonBackground: String,
        iconBackground2: String,
        iconGradientAngle: Int,
        customize: JSONObject.() -> Unit = {},
    ): Definition {
        val values = base().apply {
            put("runtimeOverlayBackgroundColor", background)
            put("runtimeOverlayBackgroundTransparency", transparency)
            put("runtimeOverlayOutlineColor", outline)
            put("runtimeOverlayOutlineWidthDp", 2)
            put("runtimeOverlayButtonText", id.take(2).uppercase())
            put("runtimeOverlayButtonTextColor", buttonTextColor)
            put("runtimeOverlayIconTextColor2", buttonTextColor)
            put("runtimeOverlayButtonBackgroundColor", buttonBackground)
            put("runtimeOverlayIconBackgroundColor2", iconBackground2)
            put("runtimeOverlayIconGradientAngle", iconGradientAngle)
            put("runtimeOverlayIconOutlineColor", outline)
            put("runtimeOverlayIconOutlineColor2", outline)
            put("runtimeOverlayMenuTextColor1", textColor)
            put("runtimeOverlayMenuTextColor2", textColor)
            put("runtimeOverlayMenuTextColor3", textColor)
            put("runtimeOverlayMenuTextColor4", textColor)
            put("runtimeOverlayMenuTextColor5", textColor)
            put("runtimeOverlayMenuTextColor6", textColor)
            put("runtimeOverlayMenuTextColor7", outline)
            put("runtimeOverlaySeparatorBackgroundColor", background)
            put("runtimeOverlaySeparatorStyle", "ascii")
            put("runtimeOverlayAppendDescriptionColor", textColor)
            put("runtimeOverlayIconBackgroundColor3", iconBackground2)
            put("runtimeOverlayIconBackgroundColor4", background)
            customize()
        }
        return Definition(id, name, configuration = values)
    }

    val definitions: List<Definition> = listOf(
        definition(
            id = "unipatches",
            name = "UniPatches",
            background = "#300000",
            transparency = 80,
            outline = "#FF5656",
            textColor = "#FF5656",
            buttonTextColor = "#FF3C00",
            buttonBackground = "#500000",
            iconBackground2 = "#AA0000",
            iconGradientAngle = 0,
        ) {
            put("runtimeOverlayButtonText", "U")
            put("runtimeOverlayIconTextGradient", true)
            put("runtimeOverlayIconTextColor2", "#FF9300")
            put("runtimeOverlayIconStyle", "parts")
            put("runtimeOverlayIconParts", JSONArray().apply {
                put("text|50|53.6|60|60|0|solid|#000000|#000000|2|45|0|U|true|default")
                put("text|50|50|60|60|0|gradient|#FF3C00|#FF9300|0|100|1|U|true|default")
            })
            put("runtimeOverlayControlBackground", "#FF5656")
            put("runtimeOverlayControlForeground", "#FF5656")
            put("runtimeOverlayBottomButtonTextColor", "#FF5656")
            put("runtimeOverlayBottomButtonBackground1", "#FF5656")
            put("runtimeOverlayBottomButtonBackground2", "#FF5656")
        },
        definition("morpheBlue", "Morphe-inspired", "#101820", 80, "#55D6BE", "#55D6BE", "#FFFFFF", "#000083", "#00AF7C", 30) {
            put("runtimeOverlayButtonText", "M")
            put("runtimeOverlayAppendDescriptionColor", "#55D6BE")
            put("runtimeOverlayControlBackground", "#55D6BE")
            put("runtimeOverlayControlForeground", "#55D6BE")
            put("runtimeOverlayBottomButtonTextColor", "#55D6BE")
            put("runtimeOverlayBottomButtonBackground1", "#55D6BE")
            put("runtimeOverlayBottomButtonBackground2", "#55D6BE")
        },
        definition("dark", "Dark", "#101010", 88, "#B0B0B0", "#FFFFFF", "#FFFFFF", "#202020", "#404040", 0) {
            put("runtimeOverlayButtonText", "D")
            put("runtimeOverlayControlBackground", "#B0B0B0")
            put("runtimeOverlayControlForeground", "#FFFFFF")
            put("runtimeOverlayBottomButtonTextColor", "#FFFFFF")
            put("runtimeOverlayBottomButtonBackground1", "#FFFFFF")
            put("runtimeOverlayBottomButtonBackground2", "#FFFFFF")
        },
        definition("light", "Light", "#F5F5F5", 92, "#202020", "#202020", "#000000", "#FFFFFF", "#DADADA", 0) {
            put("runtimeOverlayButtonText", "L")
            put("runtimeOverlayControlBackground", "#202020")
            put("runtimeOverlayControlForeground", "#202020")
            put("runtimeOverlayBottomButtonTextColor", "#202020")
            put("runtimeOverlayBottomButtonBackground1", "#202020")
            put("runtimeOverlayBottomButtonBackground2", "#202020")
        },
        definition("zarchiver", "ZArchiver-inspired", "#666666", 100, "#00A000", "#FFFFFF", "#FFFFFF", "#5BAA08", "#5BAA08", 0) {
            put("runtimeOverlayButtonText", "Z")
            put("runtimeOverlayButtonShape", "squircle")
            put("runtimeOverlayIconStyle", "parts")
            put("runtimeOverlayIconParts", JSONArray().apply {
                put("text|41|50|45|70|0|solid|#FFFFFF|#FFFFFF|0|100|0|Z|true|default")
                put("text|59|50|45|70|0|solid|#FFFFFF|#FFFFFF|0|100|1|A|true|default")
            })
            put("runtimeOverlayIconBackgroundStyle", "faceted")
            put("runtimeOverlayIconBackgroundColor3", "#3D7806")
            put("runtimeOverlayIconBackgroundColor4", "#69B90A")
            put("runtimeOverlayControlBackground", "#FFFFFF")
            put("runtimeOverlayControlForeground", "#FFFFFF")
            put("runtimeOverlayBottomButtonTextColor", "#FFFFFF")
            put("runtimeOverlayBottomButtonBackground1", "#FFFFFF")
            put("runtimeOverlayBottomButtonBackground2", "#FFFFFF")
        },
        definition("luckyPatcher", "LuckyPatcher-inspired", "#000000", 100, "#FFFF00", "#FFFFFF", "#FFFFFF", "#C6B407", "#F0E60A", 90) {
            put("runtimeOverlayButtonText", "LP")
            put("runtimeOverlayIconOutline", true)
            put("runtimeOverlayIconOutlineColor", "#000000")
            put("runtimeOverlayIconOutlineWidthDp", 2)
            put("runtimeOverlayIconStyle", "parts")
            put("runtimeOverlayIconParts", JSONArray().apply {
                put("circle|35|35|10|16|5|solid|#000000|#000000|0|100|0")
                put("circle|65|35|10|16|-5|solid|#000000|#000000|0|100|0")
                put("arc|50|57|50|40|0|solid|#000000|#000000|10|100|1")
                put("circle|27|62|8|4|-10|solid|#000000|#000000|0|100|2")
                put("circle|73|62|8|4|10|solid|#000000|#000000|0|100|2")
            })
            put("runtimeOverlayIconHighlight", true)
            put("runtimeOverlayControlBackground", "#D3D3D3")
            put("runtimeOverlayControlForeground", "#B0FFFF")
            put("runtimeOverlayBottomButtonStyle", "gradient")
            put("runtimeOverlayBottomButtonShape", "squircle")
            put("runtimeOverlayBottomButtonPadding", true)
            put("runtimeOverlayBottomButtonTextColor", "#FFFFFF")
            put("runtimeOverlayBottomButtonBackground1", "#3A5A80")
            put("runtimeOverlayBottomButtonBackground2", "#6E91B9")
            put("runtimeOverlayMenuTextColor2", "#00FF00")
            put("runtimeOverlayMenuTextColor3", "#B8B8B8")
            put("runtimeOverlayMenuTextColor4", "#D14DCA")
            put("runtimeOverlayMenuTextColor5", "#00FF00")
            put("runtimeOverlayMenuTextColor6", "#000000")
            put("runtimeOverlayMenuTextColor7", "#FFFF00")
            put("runtimeOverlaySeparatorBackgroundColor", "#D0D0D0")
            put("runtimeOverlaySeparatorStyle", "background")
            put("runtimeOverlayTitleIconPlacement", "left")
            put("runtimeOverlayTitleSeparator", true)
            put("runtimeOverlayMenuCorners", "square")
            put("runtimeOverlayOpeningAnimation", "appearRight")
            put("runtimeOverlayClosingAnimation", "disappearUp")
            put("runtimeOverlayAnimationDuration", 300)
        },
        definition("reVanced", "ReVanced-inspired", "#1B1B1D", 90, "#D5B8FF", "#B8D2FF", "#FFFFFF", "#1B1B1B", "#1B1B1B", 0) {
            put("runtimeOverlayButtonText", "RV")
            put("runtimeOverlayOutlineWidthDp", 3)
            put("runtimeOverlayAppendDescriptionColor", "#D5B8FF")
            put("runtimeOverlayIconOutline", true)
            put("runtimeOverlayIconOutlineColor", "#EF4E99")
            put("runtimeOverlayIconOutlineGradient", true)
            put("runtimeOverlayIconOutlineColor2", "#4E97F0")
            put("runtimeOverlayIconOutlineGradientAngle", 90)
            put("runtimeOverlayIconStyle", "parts")
            put("runtimeOverlayIconParts", JSONArray().apply {
                put("v|50|54|57|54|0|solid|#FFFFFF|#FFFFFF|16|100|0")
                put("roundedTriangle|50|45|25|26|0|gradient|#E651A0|#6564D3|0|100|1")
            })
            put("runtimeOverlayControlTheme", "monet")
            put("runtimeOverlayControlBackground", "#91B6F2")
            put("runtimeOverlayControlForeground", "#C5D9FF")
            put("runtimeOverlayBottomButtonStyle", "solid")
            put("runtimeOverlayBottomButtonShape", "squircle")
            put("runtimeOverlayBottomButtonPadding", true)
            put("runtimeOverlayBottomButtonTextColor", "#D5B8FF")
            put("runtimeOverlayBottomButtonBackground1", "#39304D")
            put("runtimeOverlayBottomButtonBackground2", "#39304D")
            put("runtimeOverlayMenuTextColor2", "#D5B8FF")
            put("runtimeOverlayMenuTextColor3", "#D0D0D5")
            put("runtimeOverlayMenuTextColor4", "#8DB8F5")
            put("runtimeOverlayMenuTextColor5", "#8DB8F5")
            put("runtimeOverlayMenuTextColor6", "#A990D0")
            put("runtimeOverlayMenuTextColor7", "#D5B8FF")
            put("runtimeOverlaySeparatorStyle", "inline")
            put("runtimeOverlayMenuOutlineAnimation", "gradient")
            put("runtimeOverlayOpeningAnimation", "scale")
            put("runtimeOverlayClosingAnimation", "scale")
            put("runtimeOverlayAnimationDuration", 500)
            put("runtimeOverlayAnimationEasing", "logarithmic")
        },
    )
}
