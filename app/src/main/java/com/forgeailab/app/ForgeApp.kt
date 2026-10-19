package com.forgeailab.app

import android.app.Application
import com.forgeailab.app.data.local.SettingsStore
import com.forgeailab.app.domain.ai.AiAgent
import com.forgeailab.app.domain.apk.ApkAnalyzer
import com.forgeailab.app.domain.apk.InstalledAppsLoader
import com.forgeailab.app.domain.engine.ApkInstaller
import com.forgeailab.app.domain.engine.RebuildEngine
import com.forgeailab.app.domain.engine.SignManager
import com.forgeailab.app.domain.workspace.WorkspaceManager

class ForgeApp : Application() {
    lateinit var workspace: WorkspaceManager
        private set
    lateinit var analyzer: ApkAnalyzer
        private set
    lateinit var rebuildEngine: RebuildEngine
        private set
    lateinit var signManager: SignManager
        private set
    lateinit var aiAgent: AiAgent
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var installedApps: InstalledAppsLoader
        private set
    lateinit var installer: ApkInstaller
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        workspace = WorkspaceManager(this)
        analyzer = ApkAnalyzer()
        rebuildEngine = RebuildEngine(workspace)
        signManager = SignManager(this)
        aiAgent = AiAgent(workspace, analyzer, rebuildEngine, signManager)
        settings = SettingsStore(this)
        installedApps = InstalledAppsLoader(this)
        installer = ApkInstaller(this)
    }

    companion object {
        lateinit var instance: ForgeApp
            private set
    }
}
