package com.forgeailab.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector

enum class ForgeDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Dashboard("dashboard", "داشبورد", Icons.Outlined.Home, Icons.Filled.Home),
    Analysis("analysis", "تحلیل", Icons.Outlined.Search, Icons.Filled.Search),
    Resources("resources", "منابع", Icons.Outlined.Folder, Icons.Filled.Folder),
    Studio("studio", "استودیو", Icons.Outlined.Edit, Icons.Filled.Edit),
    Components("components", "اجزاء", Icons.Outlined.Extension, Icons.Filled.Extension),
    Ai("ai", "دستیار AI", Icons.Outlined.Info, Icons.Filled.Info),
    Rebuild("rebuild", "بازسازی", Icons.Outlined.Build, Icons.Filled.Build),
    Sign("sign", "امضا", Icons.Outlined.Share, Icons.Filled.Share),
    Projects("projects", "پروژه‌ها", Icons.Outlined.List, Icons.Filled.List),
    Reports("reports", "گزارش", Icons.Outlined.Description, Icons.Filled.Description),
    Settings("settings", "تنظیمات", Icons.Outlined.Settings, Icons.Filled.Settings);
    companion object {
        val bottomPrimary = listOf(Dashboard, Analysis, Resources, Ai, Settings)
        val all = entries
    }
}
