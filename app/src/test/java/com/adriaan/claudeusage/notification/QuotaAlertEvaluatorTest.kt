package com.adriaan.claudeusage.notification

import com.adriaan.claudeusage.data.model.UsageLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotaAlertEvaluatorTest {

    private fun limit(kind: String, percent: Int, group: String = "session") =
        UsageLimit(
            kind = kind,
            group = group,
            percent = percent,
            severity = "normal",
            resetsAt = null,
            isActive = true
        )

    private val thresholds = listOf(50, 75, 90)

    @Test
    fun firesForEachCrossedThreshold_onFirstEvaluation() {
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 80)),
            thresholds = thresholds,
            previouslyFired = emptySet()
        )

        // 80% crosses 50 and 75 but not 90.
        assertEquals(setOf(50, 75), result.alerts.map { it.threshold }.toSet())
        assertTrue(result.alerts.all { it.kind == "session" && it.percent == 80 })
        assertEquals(setOf("session@50", "session@75"), result.nextFired)
    }

    @Test
    fun doesNotRefireAlreadyFiredThreshold() {
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 80)),
            thresholds = thresholds,
            previouslyFired = setOf("session@50", "session@75")
        )

        assertTrue(result.alerts.isEmpty())
        assertEquals(setOf("session@50", "session@75"), result.nextFired)
    }

    @Test
    fun firesOnlyTheNewlyCrossedThreshold() {
        // Was at 80% (50 & 75 fired); now at 95% -> only 90 is new.
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 95)),
            thresholds = thresholds,
            previouslyFired = setOf("session@50", "session@75")
        )

        assertEquals(listOf(90), result.alerts.map { it.threshold })
        assertEquals(setOf("session@50", "session@75", "session@90"), result.nextFired)
    }

    @Test
    fun reArmsAfterWindowResetsBelowThreshold() {
        // Window reset: percent dropped to 10, previously all fired.
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 10)),
            thresholds = thresholds,
            previouslyFired = setOf("session@50", "session@75", "session@90")
        )

        assertTrue(result.alerts.isEmpty())
        assertTrue("flags should be cleared after reset", result.nextFired.isEmpty())
    }

    @Test
    fun handlesMultipleWindowsIndependently() {
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(
                limit("session", 60),
                limit("weekly_all", 92, group = "weekly")
            ),
            thresholds = thresholds,
            previouslyFired = emptySet()
        )

        assertEquals(
            setOf("session@50", "weekly_all@50", "weekly_all@75", "weekly_all@90"),
            result.nextFired
        )
        assertEquals(
            setOf("session" to 50, "weekly_all" to 50, "weekly_all" to 75, "weekly_all" to 90),
            result.alerts.map { it.kind to it.threshold }.toSet()
        )
    }

    @Test
    fun prunesFlagsForWindowsThatNoLongerExist() {
        // A stale weekly flag should be dropped when only the session window is present now.
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 55)),
            thresholds = thresholds,
            previouslyFired = setOf("weekly_all@50", "session@50")
        )

        assertTrue(result.alerts.isEmpty())
        assertEquals(setOf("session@50"), result.nextFired)
    }

    @Test
    fun noThresholds_producesNoAlerts() {
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 100)),
            thresholds = emptyList(),
            previouslyFired = emptySet()
        )
        assertTrue(result.alerts.isEmpty())
        assertTrue(result.nextFired.isEmpty())
    }

    @Test
    fun exactBoundaryCounts_asCrossed() {
        val result = QuotaAlertEvaluator.evaluate(
            limits = listOf(limit("session", 50)),
            thresholds = thresholds,
            previouslyFired = emptySet()
        )
        assertEquals(listOf(50), result.alerts.map { it.threshold })
    }
}
