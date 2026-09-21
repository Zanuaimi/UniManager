package com.zanuaimi.unimanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zanuaimi.unimanager.data.AppRegistry
import org.json.JSONArray
import org.json.JSONObject

/** Editor for patch options represented as a list of strings, such as icon part definitions. */
class MultiPartStringsActivity : Activity() {
    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_KEY = "configuration_key"
        const val EXTRA_TITLE = "configuration_title"
        const val EXTRA_VALUES = "configuration_values"
    }

    private val registeredPackageName by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty() }
    private val configurationKey by lazy { intent.getStringExtra(EXTRA_KEY).orEmpty() }
    private val registry by lazy { AppRegistry(this) }
    private val editors = mutableListOf<EditText>()
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.background
        window.navigationBarColor = UiKit.background
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.background)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(UiKit.text)
            setOnClickListener { UiKit.animatePress(this); finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        toolbar.addView(UiKit.text(this, intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Multi-part strings" }, 21f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(toolbar)
        root.addView(UiKit.text(this, "Add, edit, or remove one string per icon part.", 13f, UiKit.muted))
        root.addView(UiKit.spacing(this, 12))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(list)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(action("Add string") { addEditor("") }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })
        root.addView(action("Save list") { save() }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
        setContentView(root)

        val initial = decodeValues(registry.configuration(registeredPackageName).opt(configurationKey))
        if (initial.isEmpty()) addEditor("") else initial.forEach(::addEditor)
        UiKit.animateAppear(root)
    }

    private fun addEditor(value: String) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val input = EditText(this).apply {
            setText(value)
            setTextColor(UiKit.text)
            setHintTextColor(UiKit.muted)
            hint = "Part definition"
            textSize = 13f
            setSingleLine(false)
            background = UiKit.rounded(UiKit.elevated, 12, UiKit.outline)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val remove = TextView(this).apply {
            text = "×"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(UiKit.muted)
            contentDescription = "Remove string"
            setOnClickListener {
                UiKit.animatePress(this)
                editors.remove(input)
                list.removeView(row)
            }
        }
        editors.add(input)
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(remove, LinearLayout.LayoutParams(dp(48), dp(48)))
        list.addView(row)
        UiKit.animateAppear(row, (editors.size - 1) * 25L)
    }

    private fun save() {
        val values = JSONArray()
        editors.map { it.text.toString() }.filter { it.isNotBlank() }.forEach(values::put)
        registry.updateConfiguration(registeredPackageName, JSONObject().put(configurationKey, values))
        setResult(RESULT_OK, Intent().putExtra(EXTRA_VALUES, values.toString()))
        finish()
    }

    private fun action(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(UiKit.text)
        background = UiKit.rounded(UiKit.accentDark, 16)
        setOnClickListener { UiKit.animatePress(this); onClick() }
    }

    private fun decodeValues(value: Any?): List<String> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
        is String -> runCatching {
            val array = JSONArray(value)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }.getOrElse { value.lines().filter(String::isNotBlank) }
        else -> emptyList()
    }

    private fun dp(value: Int): Int = UiKit.run { this@MultiPartStringsActivity.dp(value) }
}
