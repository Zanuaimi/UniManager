package com.zanuaimi.unimanager.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.zanuaimi.unimanager.data.InstalledAppScanner
import com.zanuaimi.unimanager.data.model.AppVisibility
import com.zanuaimi.unimanager.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    suspend fun list(visibility: AppVisibility, query: String): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { application ->
                    val system = application.isSystemApplication()
                    when (visibility) {
                        AppVisibility.USER -> !system
                        AppVisibility.SYSTEM -> system
                        AppVisibility.ALL -> true
                    }
                }
                .map { application ->
                    InstalledApp(
                        info = application,
                        label = application.loadLabel(packageManager).toString(),
                        version = runCatching {
                            packageManager.getPackageInfo(application.packageName, 0).versionName.orEmpty()
                        }.getOrDefault(""),
                        isSystem = application.isSystemApplication(),
                    )
                }
                .filter { app ->
                    query.isBlank() || app.label.contains(query, true) || app.info.packageName.contains(query, true)
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }

    suspend fun registration(app: InstalledApp): String? = withContext(Dispatchers.IO) {
        InstalledAppScanner.registrationFor(appContext, app.info)?.toString()
    }

    private fun ApplicationInfo.isSystemApplication(): Boolean =
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}
