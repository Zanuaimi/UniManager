package com.zanuaimi.unimanager

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zanuaimi.unimanager.data.AppRegistry
import org.json.JSONObject

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.protocol_summary)
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 24)
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val apps = AppRegistry(this).all()
        if (apps.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.empty_apps)
                textSize = 16f
            })
        } else {
            apps.forEach { app -> list.addView(appRow(app)) }
        }
        root.addView(ScrollView(this).apply {
            addView(list, ViewGroup.LayoutParams(-1, -1))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun appRow(app: JSONObject): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 16, 0, 16)
        setOnClickListener {
            startActivity(Intent(this@MainActivity, AppDetailsActivity::class.java).apply {
                putExtra(AppDetailsActivity.EXTRA_PACKAGE_NAME, app.optString("package_name"))
            })
        }
        val icon = ImageView(this@MainActivity).apply {
            setImageDrawable(resolveIcon(app.optString("package_name")))
            layoutParams = LinearLayout.LayoutParams(64, 64)
        }
        addView(icon)
        val text = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        text.addView(TextView(this@MainActivity).apply {
            this.text = app.optString("app_label", "Unknown app")
            this.textSize = 17f
            this.setTextColor(Color.BLACK)
        })
        text.addView(TextView(this@MainActivity).apply {
            this.text = app.optString("package_name")
            this.textSize = 13f
            this.setTextColor(Color.DKGRAY)
        })
        text.addView(TextView(this@MainActivity).apply {
            this.text = AppRegistry(this@MainActivity).status(app).label
            this.textSize = 12f
            this.setTextColor(Color.rgb(80, 80, 80))
        })
        addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = 20 })
    }

    private fun resolveIcon(packageName: String): Drawable? = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull() ?: getDrawable(R.drawable.ic_launcher)
}
