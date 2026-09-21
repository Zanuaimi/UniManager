package com.zanuaimi.unimanager

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zanuaimi.unimanager.data.AppRegistry
import org.json.JSONArray
import org.json.JSONObject

class AppDetailsActivity : Activity() {
    companion object { const val EXTRA_PACKAGE_NAME = "package_name" }

    private class SettingNode(val name: String) {
        val children = linkedMapOf<String, SettingNode>()
        val keys = mutableListOf<String>()
    }

    private val appPackageName by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty() }
    private val registry by lazy { AppRegistry(this) }
    private val controls = linkedMapOf<String, CheckBox>()
    private val textValues = linkedMapOf<String, EditText>()
    private val listValues = linkedMapOf<String, JSONArray>()
    private val pickerKeys = linkedMapOf<Int, String>()
    private val editorKeys = linkedMapOf<Int, String>()
    private lateinit var configurationContainer: LinearLayout
    private lateinit var search: EditText
    private var expandedGroups = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.background
        window.navigationBarColor = UiKit.background
        render()
    }

    private fun render() {
        val app = registry.get(appPackageName) ?: run { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.background)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setColorFilter(UiKit.text)
            background = null
            contentDescription = "Back"
            setOnClickListener { UiKit.animatePress(this); finish() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        toolbar.addView(UiKit.text(this, app.optString("app_label", "App"), 21f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(toolbar)
        root.addView(identityCard(app))
        root.addView(UiKit.spacing(this, 12))

        search = EditText(this).apply {
            hint = "Search settings"
            setHintTextColor(UiKit.muted)
            setTextColor(UiKit.text)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderConfiguration(app) }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(UiKit.spacing(this, 12))

        configurationContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(configurationContainer)
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        val save = TextView(this).apply {
            text = "Save configuration"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(UiKit.text)
            background = UiKit.rounded(UiKit.accentDark, 18)
            setOnClickListener { UiKit.animatePress(this); saveConfiguration(); finish() }
        }
        root.addView(save, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        setContentView(root)
        UiKit.animateAppear(root)
        renderConfiguration(app)
    }

    private fun identityCard(app: JSONObject): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = UiKit.rounded(UiKit.surface, 20, UiKit.outline)
        addView(UiKit.text(this@AppDetailsActivity, app.optString("package_name"), 13f, UiKit.muted))
        addView(UiKit.spacing(this@AppDetailsActivity, 6))
        val status = registry.status(app)
        addView(UiKit.text(this@AppDetailsActivity, "${status.label}  ·  ${status.detail}", 13f, if (status.kind == AppRegistry.StatusKind.READY) UiKit.accent else UiKit.muted))
        val patches = app.optJSONArray("patches")?.let { patchList ->
            (0 until patchList.length()).mapNotNull { index ->
                patchList.optJSONObject(index)?.let { patch ->
                    val id = patch.optString("id").ifBlank { "unknown" }
                    val version = patch.optString("version").ifBlank { "?" }
                    "$id.v$version"
                }
            }
        }.orEmpty()
        val capabilities = app.optJSONArray("capabilities")?.let { capabilityList ->
            (0 until capabilityList.length()).mapNotNull { index ->
                capabilityList.optString(index).takeIf(String::isNotBlank)
            }
        }.orEmpty()
        if (patches.isNotEmpty()) {
            addView(UiKit.spacing(this@AppDetailsActivity, 8))
            addView(UiKit.text(this@AppDetailsActivity, "Patches: ${patches.joinToString()}", 12f, UiKit.muted))
        }
        if (capabilities.isNotEmpty()) {
            addView(UiKit.spacing(this@AppDetailsActivity, 4))
            addView(UiKit.text(this@AppDetailsActivity, "Capabilities: ${capabilities.joinToString()}", 12f, UiKit.muted))
        }
    }

    private fun renderConfiguration(app: JSONObject) {
        controls.clear()
        textValues.clear()
        listValues.clear()
        configurationContainer.removeAllViews()
        val query = search.text.toString().trim().lowercase()
        val configuration = registry.configuration(appPackageName)
        val groups = linkedMapOf<String, MutableList<String>>()
        val genericKeys = mutableListOf<String>()
        if (registry.hasCapability(app, AppRegistry.CAPABILITY_BLOCK_ADS)) groups.getOrPut("Ads Block Patch") { mutableListOf() }.add("block_ads")
        if (registry.hasCapability(app, AppRegistry.CAPABILITY_BLOCK_HOSTS)) groups.getOrPut("Ads Block Patch") { mutableListOf() }.add("block_hosts")
        val keys = configuration.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!groups.values.any { key in it }) genericKeys.add(key)
        }
        if (groups.isEmpty() && genericKeys.isEmpty()) {
            configurationContainer.addView(notice("No configurable capabilities", "This app registered successfully, but it did not report manager-editable settings."))
            return
        }
        groups.entries.forEachIndexed { index, (group, keysInGroup) ->
            val visibleKeys = keysInGroup.filter { key ->
                query.isBlank() || key.lowercase().contains(query) || group.lowercase().contains(query) || labelFor(key).lowercase().contains(query)
            }
            if (visibleKeys.isEmpty()) return@forEachIndexed
            addGroup(group, visibleKeys, configuration, index)
        }
        val visibleGenericKeys = genericKeys.filter { key ->
            query.isBlank() || key.lowercase().contains(query) || labelFor(key).lowercase().contains(query)
        }
        if (visibleGenericKeys.isNotEmpty()) addNestedGroups(visibleGenericKeys, configuration, groups.size)
        if (configurationContainer.childCount == 0) configurationContainer.addView(notice("No matching settings", "Try a different search term."))
    }

    private fun addNestedGroups(keys: List<String>, configuration: JSONObject, offset: Int) {
        val roots = linkedMapOf<String, SettingNode>()
        keys.forEach { key ->
            val parts = key.split('.').filter { it.isNotBlank() }
            if (parts.size < 2) {
                roots.getOrPut("Other settings") { SettingNode("Other settings") }.keys.add(key)
                return@forEach
            }
            var node = roots.getOrPut(parts.first()) { SettingNode(parts.first()) }
            parts.drop(1).dropLast(1).forEach { part ->
                node = node.children.getOrPut(part) { SettingNode(part) }
            }
            node.keys.add(key)
        }
        roots.values.forEachIndexed { index, node -> addNestedGroup(configurationContainer, node, configuration, offset + index) }
    }

    private fun addNestedGroup(parent: LinearLayout, node: SettingNode, configuration: JSONObject, index: Int) {
        val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(this).apply {
            textSize = 16f
            setTextColor(UiKit.text)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(8))
            background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
        }
        node.keys.forEach { addSetting(body, it, configuration) }
        node.children.values.forEach { addNestedGroup(body, it, configuration, index + 1) }
        group.addView(header)
        group.addView(body)
        val title = labelFor(node.name)
        val initiallyExpanded = title in expandedGroups || expandedGroups.isEmpty()
        body.visibility = if (initiallyExpanded) View.VISIBLE else View.GONE
        header.text = if (initiallyExpanded) "⌄  $title" else "›  $title"
        header.setOnClickListener {
            UiKit.animatePress(header)
            val open = body.visibility != View.VISIBLE
            if (open) {
                body.alpha = 0f
                body.visibility = View.VISIBLE
                body.animate().alpha(1f).setDuration(220).start()
            } else {
                body.animate().alpha(0f).setDuration(160).withEndAction { body.visibility = View.GONE }.start()
            }
            header.text = if (open) "⌄  $title" else "›  $title"
            if (open) expandedGroups.add(title) else expandedGroups.remove(title)
        }
        parent.addView(group)
        parent.addView(UiKit.spacing(this, 10))
        UiKit.animateAppear(group, index * 35L)
    }

    private fun addGroup(title: String, keys: List<String>, configuration: JSONObject, index: Int) {
        val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(this).apply {
            textSize = 16f
            setTextColor(UiKit.text)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(8))
            background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
        }
        keys.forEach { key -> addSetting(body, key, configuration) }
        group.addView(header)
        group.addView(body)
        val initiallyExpanded = title in expandedGroups || expandedGroups.isEmpty()
        body.visibility = if (initiallyExpanded) View.VISIBLE else View.GONE
        header.text = if (initiallyExpanded) "⌄  $title" else "›  $title"
        header.setOnClickListener {
            UiKit.animatePress(header)
            val open = body.visibility != View.VISIBLE
            if (open) {
                body.alpha = 0f
                body.visibility = View.VISIBLE
                body.animate().alpha(1f).translationY(0f).setDuration(220).start()
            } else {
                body.animate().alpha(0f).setDuration(160).withEndAction { body.visibility = View.GONE }.start()
            }
            header.text = if (open) "⌄  $title" else "›  $title"
            if (open) expandedGroups.add(title) else expandedGroups.remove(title)
        }
        configurationContainer.addView(group)
        configurationContainer.addView(UiKit.spacing(this, 10))
        UiKit.animateAppear(group, index * 35L)
    }

    private fun addSetting(parent: LinearLayout, key: String, configuration: JSONObject) {
        val value = configuration.opt(key)
        if (value is Boolean || key == "block_ads" || key == "block_hosts") {
            val check = CheckBox(this).apply {
                text = labelFor(key)
                textSize = 15f
                setTextColor(UiKit.text)
                isChecked = configuration.optBoolean(key, false)
            }
            controls[key] = check
            parent.addView(check)
            return
        }
        if (isStringListSetting(key, value)) {
            addStringListSetting(parent, key, configuration)
            return
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        row.addView(UiKit.text(this, labelFor(key), 13f, UiKit.muted))
        val input = EditText(this).apply {
            setTextColor(UiKit.text)
            setSingleLine(true)
            background = UiKit.rounded(UiKit.elevated, 12, UiKit.outline)
            setPadding(dp(12), 0, dp(12), 0)
        }
        textValues[key] = input
        if (isColorSetting(key, configuration.optString(key))) {
            val colorRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val preview = View(this).apply {
                background = colorBackground(configuration.optString(key))
                contentDescription = "Color preview"
            }
            colorRow.addView(preview, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(8) })
            colorRow.addView(input, LinearLayout.LayoutParams(0, dp(46), 1f))
            input.setText(configuration.optString(key))
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    preview.background = colorBackground(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            row.addView(colorRow)
        } else {
            input.setText(configuration.optString(key))
            row.addView(input, LinearLayout.LayoutParams(-1, dp(46)))
        }
        if (key.contains("file", true) || key.contains("image", true) || key.contains("folder", true) || key.contains("path", true)) {
            row.addView(TextView(this).apply {
                text = if (key.contains("folder", true)) "Choose folder" else "Choose file"
                gravity = Gravity.CENTER
                setTextColor(UiKit.text)
                background = UiKit.rounded(UiKit.accentDark, 12)
                setOnClickListener { openPicker(key, input) }
            }, LinearLayout.LayoutParams(dp(140), dp(40)).apply { topMargin = dp(6) })
        }
        parent.addView(row)
    }

    private fun addStringListSetting(parent: LinearLayout, key: String, configuration: JSONObject) {
        val values = decodeStringList(configuration.opt(key))
        listValues[key] = values
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(UiKit.text(this, labelFor(key), 13f, UiKit.muted))
        row.addView(UiKit.text(this, "${values.length()} item(s)", 14f).apply {
            setPadding(0, dp(4), 0, dp(6))
        })
        row.addView(TextView(this).apply {
            text = "Edit list"
            gravity = Gravity.CENTER
            setTextColor(UiKit.text)
            background = UiKit.rounded(UiKit.accentDark, 12)
            setOnClickListener {
                UiKit.animatePress(this)
                val requestCode = 0x40000000 or (key.hashCode() and 0x0fffffff)
                editorKeys[requestCode] = key
                startActivityForResult(Intent(this@AppDetailsActivity, MultiPartStringsActivity::class.java).apply {
                    putExtra(MultiPartStringsActivity.EXTRA_PACKAGE_NAME, appPackageName)
                    putExtra(MultiPartStringsActivity.EXTRA_KEY, key)
                    putExtra(MultiPartStringsActivity.EXTRA_TITLE, labelFor(key))
                }, requestCode)
            }
        }, LinearLayout.LayoutParams(dp(140), dp(40)))
        parent.addView(row)
    }

    private fun saveConfiguration() {
        val values = JSONObject()
        controls.forEach { (key, value) -> values.put(key, value.isChecked) }
        textValues.forEach { (key, value) -> values.put(key, value.text.toString()) }
        listValues.forEach { (key, value) -> values.put(key, value) }
        registry.updateConfiguration(appPackageName, values)
    }

    private fun openPicker(key: String, input: EditText) {
        val intent = if (key.contains("folder", true)) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        val requestCode = key.hashCode() and 0x7fff
        pickerKeys[requestCode] = key
        startActivityForResult(intent, requestCode)
        input.hint = "Selection will appear here"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val editorKey = editorKeys.remove(requestCode)
        if (editorKey != null) {
            val encoded = data?.getStringExtra(MultiPartStringsActivity.EXTRA_VALUES).orEmpty()
            runCatching { listValues[editorKey] = JSONArray(encoded) }
            registry.get(appPackageName)?.let(::renderConfiguration)
            return
        }
        val key = pickerKeys.remove(requestCode) ?: return
        val uri = data?.data ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        textValues[key]?.setText(uri.toString())
    }

    private fun notice(title: String, message: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = UiKit.rounded(UiKit.surface, 18, UiKit.outline)
        addView(UiKit.text(this@AppDetailsActivity, title, 16f).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
        addView(UiKit.spacing(this@AppDetailsActivity, 6))
        addView(UiKit.text(this@AppDetailsActivity, message, 14f, UiKit.muted))
    }

    private fun labelFor(key: String): String = mapOf(
        "runtimeOverlayButtonTextColor" to "IconTextColor1",
        "runtimeOverlayIconTextGradient" to "Enable Icon Text Gradient",
        "runtimeOverlayIconTextColor2" to "IconTextColor2",
        "runtimeOverlayIconTextGradientAngle" to "Gradient Angle",
        "runtimeOverlayIconStyle" to "Icon type",
        "runtimeOverlayIconParts" to "Multi-part icon strings",
        "runtimeOverlayIconHighlight" to "Add highlight",
        "runtimeOverlayIconShadow" to "Drop shadow",
    )[key] ?: key.replace('.', ' ').replace('_', ' ').replace('-', ' ')
        .replaceFirstChar { it.uppercase() }

    private fun isStringListSetting(key: String, value: Any?): Boolean =
        value is JSONArray || key.contains("iconparts", true) ||
            (value is String && value.trim().startsWith("["))

    private fun decodeStringList(value: Any?): JSONArray = when (value) {
        is JSONArray -> JSONArray(value.toString())
        is String -> runCatching { JSONArray(value) }.getOrElse {
            JSONArray().also { array -> value.lines().filter(String::isNotBlank).forEach(array::put) }
        }
        else -> JSONArray()
    }

    private fun isColorSetting(key: String, value: String): Boolean =
        key.contains("color", true) || Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$").matches(value)

    private fun colorBackground(value: String): android.graphics.drawable.Drawable =
        UiKit.rounded(runCatching { Color.parseColor(value) }.getOrDefault(UiKit.elevated), 10, UiKit.outline)

    private fun dp(value: Int): Int = UiKit.run { this@AppDetailsActivity.dp(value) }
}
