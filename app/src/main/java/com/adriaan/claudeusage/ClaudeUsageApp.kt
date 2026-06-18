package com.adriaan.claudeusage

import android.app.Application
import com.adriaan.claudeusage.data.local.SessionManager
import com.adriaan.claudeusage.data.repository.UsageRepository
import com.adriaan.claudeusage.worker.UsageRefreshWorker

class ClaudeUsageApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var repository: UsageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        repository = UsageRepository(sessionManager)
        UsageRefreshWorker.schedule(this)
    }
}
