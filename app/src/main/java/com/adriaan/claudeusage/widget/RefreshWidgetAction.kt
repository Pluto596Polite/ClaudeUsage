package com.adriaan.claudeusage.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.adriaan.claudeusage.worker.UsageRefreshWorker

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Kick off an immediate one-shot refresh
        val request = OneTimeWorkRequestBuilder<UsageRefreshWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
