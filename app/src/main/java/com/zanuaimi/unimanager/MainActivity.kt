package com.zanuaimi.unimanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.zanuaimi.unimanager.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zanuaimi.unimanager.ui.navigation.UniManagerNavigation
import com.zanuaimi.unimanager.ui.theme.UniManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiKit.background
        window.navigationBarColor = UiKit.background
        setContent {
            LaunchedEffect(Unit) {
                UniManagerTheme.setAppearance(withContext(Dispatchers.IO) { SettingsRepository(applicationContext).readAppearance() })
            }
            UniManagerTheme {
                UniManagerNavigation()
            }
        }
    }
}

fun ComponentActivity.openUnknownSourcesSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
    }
}
