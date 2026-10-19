package com.forgeailab.app.domain.apk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val sourceDir: String,
    val apkSize: Long,
    val isSystem: Boolean
)

class InstalledAppsLoader(private val context: Context) {

    suspend fun load(includeSystem: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        apps.mapNotNull { ai ->
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null
            val source = ai.sourceDir ?: return@mapNotNull null
            val file = File(source)
            if (!file.exists() || !file.canRead()) return@mapNotNull null
            val label = try {
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                ai.packageName
            }
            val version = try {
                pm.getPackageInfo(ai.packageName, 0).versionName ?: "?"
            } catch (_: Exception) {
                "?"
            }
            InstalledApp(
                packageName = ai.packageName,
                label = label,
                versionName = version,
                sourceDir = source,
                apkSize = file.length(),
                isSystem = isSystem
            )
        }.sortedBy { it.label.lowercase() }
    }

    suspend fun copyApkToCache(app: InstalledApp): File = withContext(Dispatchers.IO) {
        val src = File(app.sourceDir)
        require(src.exists() && src.canRead()) { "نمی‌توان APK را خواند: ${app.packageName}" }
        val dest = File(context.cacheDir, "installed_${app.packageName.replace('.', '_')}.apk")
        src.copyTo(dest, overwrite = true)
        dest
    }
}
