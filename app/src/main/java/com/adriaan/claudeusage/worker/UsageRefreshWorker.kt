package com.adriaan.claudeusage.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adriaan.claudeusage.data.local.SessionManager
import com.adriaan.claudeusage.data.local.SettingsManager
import com.adriaan.claudeusage.data.repository.UsageRepository
import com.adriaan.claudeusage.notification.QuotaAlertNotifier
import com.adriaan.claudeusage.widget.UsageWidget
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class UsageRefreshWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(context)
        val isLoggedIn = sessionManager.isLoggedIn.first()
        if (!isLoggedIn) return Result.success()

        val repository = UsageRepository(sessionManager)
        val settingsManager = SettingsManager(context)
        val notifier = QuotaAlertNotifier(context, settingsManager)

        repository.fetchUsageData()
            .onSuccess { data ->
                // Fire any newly-crossed quota-threshold alerts.
                notifier.evaluate(data)
                // Update all pinned widgets
                UsageWidget().updateAll(context)
            }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "usage_refresh"

        fun schedule(context: Context) {
            // Android enforces a 15-minute minimum for PeriodicWork — this is the closest to 5 min allowed.
            val request = PeriodicWorkRequestBuilder<UsageRefreshWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
