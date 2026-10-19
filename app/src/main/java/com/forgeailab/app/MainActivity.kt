package com.forgeailab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.forgeailab.app.ui.navigation.ForgeDest
import com.forgeailab.app.ui.screens.ai.AiScreen
import com.forgeailab.app.ui.screens.analysis.AnalysisScreen
import com.forgeailab.app.ui.screens.components.ComponentsScreen
import com.forgeailab.app.ui.screens.dashboard.DashboardScreen
import com.forgeailab.app.ui.screens.projects.ProjectsScreen
import com.forgeailab.app.ui.screens.rebuild.RebuildScreen
import com.forgeailab.app.ui.screens.reports.ReportsScreen
import com.forgeailab.app.ui.screens.resources.ResourcesScreen
import com.forgeailab.app.ui.screens.settings.SettingsScreen
import com.forgeailab.app.ui.screens.sign.SignScreen
import com.forgeailab.app.ui.screens.studio.StudioScreen
import com.forgeailab.app.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark by ForgeApp.instance.settings.darkTheme.collectAsStateWithLifecycle(initialValue = true)
            ForgeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ForgeRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgeRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ForgeDest.Dashboard.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FORGE AI LAB", style = MaterialTheme.typography.titleMedium)
                        Text("سامانه تحلیل و مهندسی APK", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                ForgeDest.bottomPrimary.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(ForgeDest.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) dest.iconSelected else dest.icon, dest.label) },
                        label = { Text(dest.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = ForgeDest.Dashboard.route, modifier = Modifier.padding(padding)) {
            composable(ForgeDest.Dashboard.route) {
                DashboardScreen(
                    onOpenAnalysis = { nav.navigate(ForgeDest.Analysis.route) },
                    onOpenAi = { nav.navigate(ForgeDest.Ai.route) },
                    onOpenProjects = { nav.navigate(ForgeDest.Projects.route) },
                    onOpenComponents = { nav.navigate(ForgeDest.Components.route) },
                    onOpenRebuild = { nav.navigate(ForgeDest.Rebuild.route) },
                    onOpenSign = { nav.navigate(ForgeDest.Sign.route) },
                    onOpenStudio = { nav.navigate(ForgeDest.Studio.route) },
                    onOpenReports = { nav.navigate(ForgeDest.Reports.route) }
                )
            }
            composable(ForgeDest.Analysis.route) { AnalysisScreen() }
            composable(ForgeDest.Resources.route) { ResourcesScreen() }
            composable(ForgeDest.Studio.route) { StudioScreen() }
            composable(ForgeDest.Components.route) { ComponentsScreen() }
            composable(ForgeDest.Ai.route) { AiScreen() }
            composable(ForgeDest.Rebuild.route) { RebuildScreen() }
            composable(ForgeDest.Sign.route) { SignScreen() }
            composable(ForgeDest.Projects.route) { ProjectsScreen() }
            composable(ForgeDest.Reports.route) { ReportsScreen() }
            composable(ForgeDest.Settings.route) { SettingsScreen() }
        }
    }
}
