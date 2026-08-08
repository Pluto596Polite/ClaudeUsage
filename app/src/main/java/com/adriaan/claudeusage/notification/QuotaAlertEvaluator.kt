package com.adriaan.claudeusage.notification

import com.adriaan.claudeusage.data.model.UsageLimit

/**
 * Pure decision logic for quota alerts, separated from Android/notification side effects so it can
 * be unit-tested.
 *
 * Given the current usage windows, the user's thresholds, and the set of flags that have already
 * fired, it decides which brand-new threshold crossings should notify and returns the updated flag
 * set. A flag re-arms (is removed) once its window falls back below the threshold, and flags for
 * windows/thresholds that no longer exist are pruned so the set can't grow unbounded.
 */
object QuotaAlertEvaluator {

    /** A single threshold crossing that warrants a notification. */
    data class Alert(val kind: String, val threshold: Int, val percent: Int)

    /** Outcome of an evaluation: the alerts to post and the flag set to persist. */
    data class Result(val alerts: List<Alert>, val nextFired: Set<String>)

    fun flagKey(kind: String, threshold: Int): String = "$kind@$threshold"

    fun evaluate(
        limits: List<UsageLimit>,
        thresholds: List<Int>,
        previouslyFired: Set<String>
    ): Result {
        val nextFired = previouslyFired.toMutableSet()
        val alerts = mutableListOf<Alert>()

        for (limit in limits) {
            for (threshold in thresholds) {
                val flag = flagKey(limit.kind, threshold)
                val crossed = limit.percent >= threshold

                if (crossed && flag !in previouslyFired) {
                    alerts.add(Alert(limit.kind, threshold, limit.percent))
                    nextFired.add(flag)
                } else if (!crossed && flag in previouslyFired) {
                    nextFired.remove(flag)
                }
            }
        }

        // Prune flags for windows/thresholds that no longer exist.
        val validFlags = limits.flatMap { l -> thresholds.map { flagKey(l.kind, it) } }.toSet()
        nextFired.retainAll(validFlags)

        return Result(alerts, nextFired)
    }
}
