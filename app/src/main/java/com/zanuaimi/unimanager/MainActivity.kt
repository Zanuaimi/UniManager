package com.zanuaimi.unimanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.zanuaimi.unimanager.data.AppRegistry
import com.zanuaimi.unimanager.data.InstalledAppScanner
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private companion object {
        const val NAV_LABEL_TAG = 0x1001
        const val NAV_ICON_TAG = 0x1002
        const val REPOSITORY_URL = "https://github.com/Zanuaimi/UniManager"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/Zanuaimi/UniManager/releases/latest"
    }

    private enum class Tab { APPS, BACKUPS, SETTINGS, ABOUT }

    private val registry by lazy { AppRegistry(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val scanExecutor = Executors.newSingleThreadExecutor()
    private val updateExecutor = Executors.newSingleThreadExecutor()
    private var selectedTab = Tab.APPS
    private lateinit var content: FrameLayout
    private lateinit var navigation: LinearLayout
    private var updateStatus: TextView? = null
    private var updateButton: TextView? = null
    private var latestApkUrl: String? = null
    private var updateCheckInFlight = false
    private var scanInFlight = false
    private var appsRefreshIndicator: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.background
        window.navigationBarColor = UiKit.background
        window.decorView.systemUiVisibility = 0
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) {
            refreshRegistryInBackground()
        }
    }

    override fun onDestroy() {
        scanExecutor.shutdownNow()
        updateExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun refreshRegistryInBackground(force: Boolean = false) {
        if (scanInFlight) return
        if (force && !getSharedPreferences(InstalledAppScanner.SETTINGS_NAME, MODE_PRIVATE)
                .getBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, true)
        ) return
        scanInFlight = true
        appsRefreshIndicator?.visibility = View.VISIBLE
        scanExecutor.execute {
            runCatching {
                if (force) InstalledAppScanner.scan(applicationContext)
                else InstalledAppScanner.scanIfNeeded(applicationContext)
            }
            runOnUiThread {
                scanInFlight = false
                if (!isFinishing && !isDestroyed) renderContent()
            }
        }
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.background)
            setPadding(dp(20), dp(18), dp(20), dp(8))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header.addView(UiKit.text(this, "UniManager", 28f).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
        header.addView(UiKit.text(this, "Your patched apps, settings, and runtime profiles", 13f, UiKit.muted))
        root.addView(header)
        root.addView(UiKit.spacing(this, 18))

        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        navigation = buildNavigation()
        root.addView(navigation, LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)
        renderContent()
    }

    private fun renderContent() {
        content.animate().cancel()
        val swap = {
            content.removeAllViews()
            when (selectedTab) {
                Tab.APPS -> renderApps()
                Tab.BACKUPS -> renderPlaceholder("Backups", "Coming Soon")
                Tab.SETTINGS -> renderSettings()
                Tab.ABOUT -> renderAbout()
            }
            updateNavigation()
            UiKit.animateAppear(content)
        }
        if (content.childCount == 0) swap() else content.animate().alpha(0f).setDuration(120).withEndAction(swap).start()
    }

    private fun renderApps() {
        val appsRoot = FrameLayout(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            var downY = 0f
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        val pulledDown = event.rawY - downY > dp(72)
                        val pullRefreshEnabled = getSharedPreferences(
                            InstalledAppScanner.SETTINGS_NAME,
                            MODE_PRIVATE,
                        ).getBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, true)
                        if (pulledDown && scrollY == 0 && !scanInFlight && pullRefreshEnabled) {
                            UiKit.animatePress(this)
                            refreshRegistryInBackground(force = true)
                            true
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        downY = 0f
                        false
                    }
                    else -> false
                }
            }
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(UiKit.text(this, "Registered apps", 20f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        list.addView(UiKit.spacing(this, 10))
        val apps = registry.all()
        if (apps.isEmpty()) {
            list.addView(noticeCard("No managed apps yet", "Patch an app with a UniPatches patch that has UniManager integration enabled. It will appear here automatically.").also { UiKit.animateAppear(it, 80) })
        } else {
            apps.forEachIndexed { index, app ->
                list.addView(appRow(app).also { UiKit.animateAppear(it, 70L + index * 45L) })
                list.addView(UiKit.spacing(this, 10))
            }
        }
        scroll.addView(list)
        appsRoot.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        appsRefreshIndicator = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = if (scanInFlight) View.VISIBLE else View.GONE
            contentDescription = "Refreshing apps"
        }
        appsRoot.addView(appsRefreshIndicator, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(8)
        })
        content.addView(appsRoot, FrameLayout.LayoutParams(-1, -1))
    }

    private fun noticeCard(title: String, message: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = UiKit.rounded(UiKit.surface, 20, UiKit.outline)
        addView(UiKit.text(this@MainActivity, title, 17f).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
        addView(UiKit.spacing(this@MainActivity, 6))
        addView(UiKit.text(this@MainActivity, message, 14f, UiKit.muted))
    }

    private fun appRow(app: JSONObject): View {
        val packageName = app.optString("package_name")
        val card = FrameLayout(this).apply {
            background = UiKit.rounded(UiKit.surface, 20, UiKit.outline)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(ImageView(this).apply {
            setImageDrawable(resolveIcon(packageName))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "${app.optString("app_label").ifBlank { "App" }} icon"
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54))
        })
        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(64), 0)
        }
        text.addView(UiKit.text(this, app.optString("app_label").ifBlank { "Unknown app" }, 17f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        val metadata = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metadata.addView(UiKit.text(this@MainActivity, packageName, 12f, UiKit.subtle), LinearLayout.LayoutParams(0, -2, 1f))
        metadata.addView(UiKit.text(this@MainActivity, appVersion(app), 12f, UiKit.subtle).apply {
            gravity = Gravity.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        text.addView(metadata)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(row, FrameLayout.LayoutParams(-1, -2))

        val config = TextView(this).apply {
            this.text = "⚙"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(UiKit.text)
            background = UiKit.rounded(UiKit.accentDark, 50)
            contentDescription = "Configure ${app.optString("app_label")}"
            setOnClickListener { UiKit.animatePress(this); openDetails(packageName) }
        }
        card.addView(config, FrameLayout.LayoutParams(dp(46), dp(46), Gravity.END or Gravity.CENTER_VERTICAL))

        val delete = Button(this).apply {
            this.text = "Delete"
            textSize = 12f
            setTextColor(UiKit.text)
            background = UiKit.rounded(android.graphics.Color.rgb(135, 20, 20), 14)
            visibility = View.GONE
            setOnClickListener { confirmDelete(app) }
        }
        card.addView(delete, FrameLayout.LayoutParams(dp(92), dp(46), Gravity.END or Gravity.CENTER_VERTICAL))

        var downX = 0f
        var revealed = false
        card.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; false }
                MotionEvent.ACTION_UP -> {
                    val distance = event.rawX - downX
                    if (distance < -dp(70)) {
                        revealed = true
                        delete.alpha = 0f
                        delete.visibility = View.VISIBLE
                        config.animate().alpha(0f).setDuration(120).withEndAction { config.visibility = View.GONE }.start()
                        delete.animate().alpha(1f).setDuration(180).start()
                        handler.postDelayed({
                            if (revealed) {
                                revealed = false
                                delete.animate().alpha(0f).setDuration(150).withEndAction { delete.visibility = View.GONE }.start()
                                config.alpha = 0f
                                config.visibility = View.VISIBLE
                                config.animate().alpha(1f).setDuration(150).start()
                            }
                        }, 5000)
                        true
                    } else if (distance > dp(70) && revealed) {
                        revealed = false
                        delete.visibility = View.GONE
                        config.alpha = 0f
                        config.visibility = View.VISIBLE
                        config.animate().alpha(1f).setDuration(150).start()
                        true
                    } else false
                }
                else -> false
            }
        }
        return card
    }

    private fun confirmDelete(app: JSONObject) {
        AlertDialog.Builder(this)
            .setTitle("Remove app entry?")
            .setMessage("This removes the UniManager profile for ${app.optString("app_label", "this app")}. It does not uninstall the app or undo its patch.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                registry.remove(app.optString("package_name"))
                InstalledAppScanner.invalidate(this)
                renderContent()
            }
            .show()
    }

    private fun buildNavigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = UiKit.rounded(UiKit.surface, 24, UiKit.outline)
        setPadding(dp(6), dp(6), dp(6), dp(6))
        listOf(
            Tab.APPS to ("▣" to "Apps"),
            Tab.BACKUPS to ("▤" to "Backups"),
            Tab.SETTINGS to ("⚙" to "Settings"),
            Tab.ABOUT to ("ⓘ" to "About"),
        ).forEach { (tab, labels) ->
            addView(TextView(this@MainActivity).apply {
                tag = tab
                setTag(NAV_LABEL_TAG, labels.second)
                setTag(NAV_ICON_TAG, labels.first)
                text = labels.first
                textSize = 13f
                gravity = Gravity.CENTER
                contentDescription = labels.second
                setOnClickListener { UiKit.animatePress(this); selectedTab = tab; renderContent() }
            }, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
    }

    private fun updateNavigation() {
        for (index in 0 until navigation.childCount) {
            val item = navigation.getChildAt(index) as TextView
            val selected = item.tag == selectedTab
            val label = item.getTag(NAV_LABEL_TAG) as String
            val icon = item.getTag(NAV_ICON_TAG) as String
            item.text = if (selected) "$icon  $label" else icon
            item.setTextColor(if (selected) UiKit.text else UiKit.muted)
            item.background = if (selected) UiKit.rounded(UiKit.accentDark, 18) else null
            item.typeface = if (selected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
    }

    private fun renderPlaceholder(title: String, message: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        box.addView(UiKit.text(this, title, 22f).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER })
        box.addView(UiKit.spacing(this, 8))
        box.addView(UiKit.text(this, message, 15f, UiKit.muted).apply { gravity = Gravity.CENTER })
        content.addView(box, FrameLayout.LayoutParams(-1, -1))
    }

    private fun renderSettings() {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        column.addView(UiKit.text(this, "Settings", 22f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        column.addView(UiKit.spacing(this, 12))

        val refreshBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = UiKit.rounded(UiKit.surface, 22, UiKit.outline)
        }
        refreshBox.addView(UiKit.text(this, "Refresh feature", 18f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        refreshBox.addView(UiKit.spacing(this, 8))

        val settings = getSharedPreferences(InstalledAppScanner.SETTINGS_NAME, MODE_PRIVATE)
        addRefreshToggle(
            refreshBox,
            "Enable Refresh upon pulling down from top of list",
            "Allow a manual pull-down gesture to scan for newly registered or updated patched apps.",
            settings.getBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, true),
        ) { enabled ->
            settings.edit().putBoolean(InstalledAppScanner.KEY_ENABLE_PULL_REFRESH, enabled).apply()
        }
        refreshBox.addView(UiKit.spacing(this, 10))
        addRefreshToggle(
            refreshBox,
            "Enable automatic refreshing",
            "Automatically scan installed apps when UniManager opens or returns to the foreground.",
            settings.getBoolean(InstalledAppScanner.KEY_ENABLE_AUTO_REFRESH, true),
        ) { enabled ->
            settings.edit().putBoolean(InstalledAppScanner.KEY_ENABLE_AUTO_REFRESH, enabled).apply()
        }
        refreshBox.addView(UiKit.spacing(this, 16))
        refreshBox.addView(UiKit.text(this, "Automatic refresh cooldown", 15f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        refreshBox.addView(UiKit.text(this, "Wait this long between automatic scans. Manual refresh is not limited by this cooldown.", 12f, UiKit.subtle))

        val cooldownRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val cooldownValue = EditText(this).apply {
            setText(settings.getLong(InstalledAppScanner.KEY_AUTO_REFRESH_VALUE, 10L).toString())
            textSize = 16f
            setTextColor(UiKit.text)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            hint = "10"
            background = UiKit.rounded(UiKit.elevated, 12, UiKit.outline)
            setPadding(dp(12), 0, dp(12), 0)
        }
        val cooldownUnit = Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("s", "m", "h", "d"),
            )
            setSelection(listOf("s", "m", "h", "d").indexOf(settings.getString(InstalledAppScanner.KEY_AUTO_REFRESH_UNIT, "m")))
        }
        cooldownRow.addView(cooldownValue, LinearLayout.LayoutParams(0, dp(52), 1f).apply { topMargin = dp(10) })
        cooldownRow.addView(cooldownUnit, LinearLayout.LayoutParams(dp(76), dp(52)).apply {
            leftMargin = dp(8)
            topMargin = dp(10)
        })
        refreshBox.addView(cooldownRow)

        fun saveCooldown() {
            val value = cooldownValue.text.toString().toLongOrNull()
            val unit = cooldownUnit.selectedItem?.toString().orEmpty()
            if (value == null || value < InstalledAppScanner.MIN_REFRESH_COOLDOWN_SECONDS) {
                cooldownValue.error = "Enter at least 30 seconds"
                return
            }
            settings.edit()
                .putLong(InstalledAppScanner.KEY_AUTO_REFRESH_VALUE, value)
                .putString(InstalledAppScanner.KEY_AUTO_REFRESH_UNIT, unit)
                .apply()
            cooldownValue.error = null
        }
        cooldownValue.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveCooldown() }
        cooldownValue.setOnEditorActionListener { _, _, _ -> saveCooldown(); false }
        cooldownUnit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = saveCooldown()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        refreshBox.addView(UiKit.text(this, "Valid range: 30 seconds or longer.", 12f, UiKit.subtle).apply {
            setPadding(0, dp(6), 0, 0)
        })
        column.addView(refreshBox)
        scroll.addView(column)
        content.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        UiKit.animateAppear(refreshBox)
    }

    private fun addRefreshToggle(
        parent: LinearLayout,
        title: String,
        description: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit,
    ): CheckBox = CheckBox(this).apply {
        text = title
        textSize = 15f
        setTextColor(UiKit.text)
        isChecked = checked
        setPadding(0, 0, 0, 0)
        setOnCheckedChangeListener { _, enabled -> onChanged(enabled) }
        parent.addView(this)
        parent.addView(UiKit.text(this@MainActivity, description, 12f, UiKit.subtle).apply {
            setPadding(dp(48), 0, 0, 0)
        })
    }

    private fun renderAbout() {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = UiKit.rounded(UiKit.surface, 22, UiKit.outline)
        }
        identity.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, LinearLayout.LayoutParams(dp(92), dp(92)))
        identity.addView(UiKit.text(this, getString(R.string.app_name), 22f).apply {
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        identity.addView(UiKit.text(this, "Version ${BuildConfig.VERSION_NAME}", 14f, UiKit.muted).apply {
            gravity = Gravity.CENTER
        })
        val repository = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(12), 0)
            background = UiKit.rounded(android.graphics.Color.BLACK, 16)
            setOnClickListener {
                UiKit.animatePress(this)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
            }
        }
        repository.addView(UiKit.text(this@MainActivity, "UniManager repository", 14f), LinearLayout.LayoutParams(0, dp(44), 1f))
        repository.addView(ImageView(this).apply { setImageResource(R.drawable.ic_github) }, LinearLayout.LayoutParams(dp(30), dp(30)))
        identity.addView(repository, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(14) })
        column.addView(identity, LinearLayout.LayoutParams(-1, -2))
        column.addView(UiKit.spacing(this, 12))

        val updates = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = UiKit.rounded(UiKit.surface, 22, UiKit.outline)
        }
        updates.addView(UiKit.text(this, "Updates", 18f).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        updateStatus = UiKit.text(this, "Checking for updates...", 14f, UiKit.muted).also { updates.addView(it) }
        updateButton = TextView(this).apply {
            text = "Update now"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(UiKit.text)
            background = UiKit.rounded(UiKit.accentDark, 16)
            visibility = View.GONE
            setOnClickListener { UiKit.animatePress(this); beginUpdate() }
        }
        updates.addView(updateButton, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        column.addView(updates, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(column)
        content.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        UiKit.animateAppear(column)
        checkForUpdates()
    }

    private fun checkForUpdates() {
        if (updateCheckInFlight) return
        updateCheckInFlight = true
        updateExecutor.execute {
            val result = runCatching {
                val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "UniManager/${BuildConfig.VERSION_NAME}")
                }
                try {
                    check(connection.responseCode in 200..299) { "GitHub returned HTTP ${connection.responseCode}" }
                    connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                } finally {
                    connection.disconnect()
                }
            }
            runOnUiThread {
                updateCheckInFlight = false
                val release = result.getOrNull()
                val latest = release?.optString("tag_name").orEmpty().removePrefix("v")
                latestApkUrl = release?.optJSONArray("assets")?.let { assets ->
                    (0 until assets.length()).firstNotNullOfOrNull { index ->
                        assets.optJSONObject(index)?.takeIf { it.optString("name") == "UniManager.apk" }
                            ?.optString("browser_download_url")
                    }
                }
                when {
                    release == null -> updateStatus?.text = "Unable to check for updates right now."
                    latestApkUrl.isNullOrBlank() -> updateStatus?.text = "No installable update is available."
                    compareVersions(latest, BuildConfig.VERSION_NAME) > 0 -> {
                        updateStatus?.text = "Update available: version $latest"
                        updateButton?.visibility = View.VISIBLE
                    }
                    else -> updateStatus?.text = "You are up to date."
                }
            }
        }
    }

    private fun beginUpdate() {
        val url = latestApkUrl ?: return
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(this)
                .setTitle("Allow APK installation")
                .setMessage("Enable Install unknown apps for UniManager, then tap Update now again.")
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        updateButton?.apply { isEnabled = false; text = "Downloading..." }
        updateExecutor.execute {
            val file = runCatching { downloadUpdate(url) }.getOrNull()
            runOnUiThread {
                if (file == null) {
                    updateStatus?.text = "Download failed. Please try again."
                    updateButton?.apply { isEnabled = true; text = "Update now" }
                } else {
                    updateStatus?.text = "Download complete. Opening installer..."
                    installUpdate(file)
                }
            }
        }
    }

    private fun downloadUpdate(url: String): File {
        val directory = File(cacheDir, "updates").apply { mkdirs() }
        val temporary = File(directory, "UniManager.apk.download")
        val target = File(directory, "UniManager.apk")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UniManager/${BuildConfig.VERSION_NAME}")
        }
        try {
            check(connection.responseCode in 200..299) { "Update download returned HTTP ${connection.responseCode}" }
            connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
            if (target.exists() && !target.delete()) error("Could not replace previous update")
            if (!temporary.renameTo(target)) error("Could not prepare downloaded APK")
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
        return target
    }

    private fun installUpdate(file: File) {
        val uri = Uri.parse("content://com.zanuaimi.unimanager.fileprovider/${file.name}")
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun compareVersions(first: String, second: String): Int {
        val a = first.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val b = second.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun openDetails(packageName: String) {
        startActivity(Intent(this, AppDetailsActivity::class.java).apply {
            putExtra(AppDetailsActivity.EXTRA_PACKAGE_NAME, packageName)
        })
    }

    private fun appVersion(app: JSONObject): String = listOf("app_version", "version_name", "version")
        .firstNotNullOfOrNull { key -> app.optString(key).takeIf { it.isNotBlank() } }
        ?: "Version unknown"

    private fun resolveIcon(packageName: String): Drawable = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrElse { getDrawable(android.R.drawable.sym_def_app_icon)!! }

    private fun dp(value: Int): Int = UiKit.run { this@MainActivity.dp(value) }

}
