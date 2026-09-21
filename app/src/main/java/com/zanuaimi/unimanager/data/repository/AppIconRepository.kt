package com.zanuaimi.unimanager.data.repository

import android.content.Context
import android.graphics.drawable.Drawable

class AppIconRepository(context: Context) {
    private val packageManager = context.applicationContext.packageManager
    private val fallback = context.getDrawable(android.R.drawable.sym_def_app_icon)

    fun load(packageName: String): Drawable? = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull() ?: fallback
}
