package com.adriaan.claudeusage

import android.app.Application
import com.adriaan.claudeusage.data.local.SessionManager
import com.adriaan.claudeusage.data.local.SettingsManager
import com.adriaan.claudeusage.data.repository.UsageRepository
import com.adriaan.claudeusage.notification.NotificationHelper
import com.adriaan.claudeusage.notification.QuotaAlertNotifier
import com.adriaan.claudeusage.worker.UsageRefreshWorker

class ClaudeUsageApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var repository: UsageRepository
        private set

    lateinit var quotaAlertNotifier: QuotaAlertNotifier
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        settingsManager = SettingsManager(this)
        repository = UsageRepository(sessionManager)
        quotaAlertNotifier = QuotaAlertNotifier(this, settingsManager)
        NotificationHelper.ensureChannel(this)
        UsageRefreshWorker.schedule(this)
    }
}
