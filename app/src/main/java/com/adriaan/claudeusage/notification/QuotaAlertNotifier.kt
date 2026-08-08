package com.adriaan.claudeusage.notification

import android.content.Context
import com.adriaan.claudeusage.data.local.SettingsManager
import com.adriaan.claudeusage.data.model.UsageData
import com.adriaan.claudeusage.data.model.UsageLimit

/**
 * Compares freshly fetched usage against the user's configured thresholds and posts a phone
 * notification for each newly-crossed threshold.
 *
 * "Newly crossed" is tracked with persisted fired-flags so a threshold that stays exceeded does
 * not re-notify on every background sync. When a window's percent drops back below a threshold
 * (its window reset), the flag is cleared and the alert re-arms.
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
        val nextFired = previouslyFired.toMutableSet()

        for (limit in data.limits) {
            for (threshold in thresholds) {
                val flag = flagKey(limit.kind, threshold)
                val crossed = limit.percent >= threshold

                if (crossed && flag !in previouslyFired) {
                    NotificationHelper.notifyThresholdCrossed(
                        context = context,
                        notificationId = flag.hashCode(),
                        title = "${windowLabel(limit.kind)} at ${limit.percent}%",
                        message = buildMessage(limit, threshold)
                    )
                    nextFired.add(flag)
                } else if (!crossed && flag in previouslyFired) {
                    // Window dropped below this threshold (reset) — re-arm the alert.
                    nextFired.remove(flag)
                }
            }
        }

        // Drop flags for thresholds/windows that no longer exist so the set can't grow unbounded.
        val validFlags = data.limits.flatMap { l -> thresholds.map { flagKey(l.kind, it) } }.toSet()
        nextFired.retainAll(validFlags)

        if (nextFired != previouslyFired) {
            settingsManager.setFiredFlags(nextFired)
        }
    }

    private fun buildMessage(limit: UsageLimit, threshold: Int): String {
        val base = "You've used ${limit.percent}% of your ${windowLabel(limit.kind).lowercase()} " +
            "quota (alert set at $threshold%)."
        return base
    }

    private fun windowLabel(kind: String): String = when (kind) {
        "session" -> "Current session"
        "weekly_all" -> "Weekly (all models)"
        "weekly_opus" -> "Weekly (Opus)"
        else -> kind.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun flagKey(kind: String, threshold: Int): String = "$kind@$threshold"
}
