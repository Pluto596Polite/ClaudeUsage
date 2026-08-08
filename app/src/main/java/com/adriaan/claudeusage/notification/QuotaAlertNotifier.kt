package com.adriaan.claudeusage.notification

import android.content.Context
import com.adriaan.claudeusage.data.local.SettingsManager
import com.adriaan.claudeusage.data.model.UsageData

/**
 * Compares freshly fetched usage against the user's configured thresholds and posts a phone
 * notification for each newly-crossed threshold.
 *
 * The decision of *which* thresholds are newly crossed lives in [QuotaAlertEvaluator] (pure and
 * unit-tested); this class only reads settings, performs the notification side effects, and
 * persists the updated flag set.
 */
class QuotaAlertNotifier(
    private val context: Context,
    private val settingsManager: SettingsManager
) {

    suspend fun evaluate(data: UsageData) {
        if (data.limits.isEmpty()) return
        if (!settingsManager.getNotificationsEnabled()) return

        val thresholds = settingsManager.getThresholds()
        if (thresholds.isEmpty()) return

        val previouslyFired = settingsManager.getFiredFlags()
        val result = QuotaAlertEvaluator.evaluate(data.limits, thresholds, previouslyFired)

        for (alert in result.alerts) {
            NotificationHelper.notifyThresholdCrossed(
                context = context,
                notificationId = QuotaAlertEvaluator.flagKey(alert.kind, alert.threshold).hashCode(),
                title = "${windowLabel(alert.kind)} at ${alert.percent}%",
                message = buildMessage(alert)
            )
        }

        if (result.nextFired != previouslyFired) {
            settingsManager.setFiredFlags(result.nextFired)
        }
    }

    private fun buildMessage(alert: QuotaAlertEvaluator.Alert): String =
        "You've used ${alert.percent}% of your ${windowLabel(alert.kind).lowercase()} " +
            "quota (alert set at ${alert.threshold}%)."

    private fun windowLabel(kind: String): String = when (kind) {
        "session" -> "Current session"
        "weekly_all" -> "Weekly (all models)"
        "weekly_opus" -> "Weekly (Opus)"
        else -> kind.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}
