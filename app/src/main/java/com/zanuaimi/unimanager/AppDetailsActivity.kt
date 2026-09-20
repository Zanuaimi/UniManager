package com.zanuaimi.unimanager

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zanuaimi.unimanager.data.AppRegistry
import org.json.JSONArray
import org.json.JSONObject

class AppDetailsActivity : Activity() {
    companion object { const val EXTRA_PACKAGE_NAME = "package_name" }

    private val appPackageName by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty() }
    private val registry by lazy { AppRegistry(this) }
    private val controls = linkedMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val app = registry.get(appPackageName) ?: run { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = app.optString("app_label", "Unknown app")
            textSize = 26f
            setTextColor(Color.BLACK)
        })
        root.addView(TextView(this).apply {
            text = appPackageName
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 4, 0, 16)
        })

        val status = registry.status(app)
        root.addView(TextView(this).apply {
            text = "${status.label}: ${status.detail}"
            textSize = 14f
            setTextColor(if (status.kind == AppRegistry.StatusKind.READY) Color.DKGRAY else Color.rgb(170, 70, 0))
            setPadding(0, 0, 0, 16)
        })

        root.addView(TextView(this).apply {
            text = "Patches\n${formatJsonArray(app.optJSONArray("patches"))}\n\nCapabilities\n${formatJsonArray(app.optJSONArray("capabilities"))}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 20)
        })

        val configuration = registry.configuration(appPackageName)
        addCapabilityControl(root, app, AppRegistry.CAPABILITY_BLOCK_ADS, "Block Ads", "block_ads", configuration)
        addCapabilityControl(root, app, AppRegistry.CAPABILITY_BLOCK_HOSTS, "Block ad and tracking hosts", "block_hosts", configuration)

        val save = Button(this).apply {
            text = "Save startup configuration"
            setOnClickListener {
                val values = JSONObject()
                controls.forEach { (key, checkBox) -> values.put(key, checkBox.isChecked) }
                registry.updateConfiguration(appPackageName, values)
                finish()
            }
        }
        root.addView(save)
        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(-1, -1))
        })
    }

    private fun addCapabilityControl(
        root: LinearLayout,
        app: JSONObject,
        capability: String,
        title: String,
        key: String,
        configuration: JSONObject,
    ) {
        if (!registry.hasCapability(app, capability)) return
        val checkBox = CheckBox(this).apply {
            text = title
            isChecked = configuration.optBoolean(key, false)
        }
        controls[key] = checkBox
        root.addView(checkBox)
    }

    private fun formatJsonArray(values: JSONArray?): String {
        if (values == null || values.length() == 0) return "None"
        return (0 until values.length()).joinToString("\n") { index ->
            values.opt(index)?.toString() ?: "Unknown"
        }
    }
}
