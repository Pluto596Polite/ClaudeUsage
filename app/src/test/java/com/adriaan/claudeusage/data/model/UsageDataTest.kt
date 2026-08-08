package com.adriaan.claudeusage.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageDataTest {

    @Test
    fun parseLimits_parsesWellFormedJson() {
        val json = """
            {"limits":[
              {"kind":"session","group":"session","percent":42,"severity":"normal","resets_at":"2026-01-01T00:00:00Z","is_active":true},
              {"kind":"weekly_all","group":"weekly","percent":80,"severity":"warning","resets_at":"2026-01-07T00:00:00Z","is_active":false}
            ]}
        """.trimIndent()

        val limits = UsageData.parseLimits(json)

        assertEquals(2, limits.size)
        val session = limits[0]
        assertEquals("session", session.kind)
        assertEquals("session", session.group)
        assertEquals(42, session.percent)
        assertEquals("normal", session.severity)
        assertEquals("2026-01-01T00:00:00Z", session.resetsAt)
        assertTrue(session.isActive)

        val weekly = limits[1]
        assertEquals("weekly_all", weekly.kind)
        assertEquals(80, weekly.percent)
        assertEquals("warning", weekly.severity)
    }

    @Test
    fun parseLimits_defaultsSeverityAndNullResets() {
        val json = """{"limits":[{"kind":"session","group":"session","percent":10}]}"""
        val limits = UsageData.parseLimits(json)
        assertEquals(1, limits.size)
        assertEquals("normal", limits[0].severity)
        assertNull(limits[0].resetsAt)
    }

    @Test
    fun parseLimits_badInput_returnsEmpty() {
        assertTrue(UsageData.parseLimits(null).isEmpty())
        assertTrue(UsageData.parseLimits("").isEmpty())
        assertTrue(UsageData.parseLimits("not json").isEmpty())
        assertTrue(UsageData.parseLimits("""{"other":123}""").isEmpty())
    }

    @Test
    fun fraction_isClampedZeroToOne() {
        assertEquals(0.5f, limit(percent = 50).fraction, 0.0001f)
        assertEquals(1f, limit(percent = 150).fraction, 0.0001f)
        assertEquals(0f, limit(percent = -5).fraction, 0.0001f)
    }

    @Test
    fun accessors_selectSessionWeeklyAndPrimary() {
        val session = limit(kind = "session", group = "session", isActive = false)
        val weekly = limit(kind = "weekly_all", group = "weekly", isActive = true)
        val data = UsageData(limits = listOf(session, weekly))

        assertEquals(session, data.sessionLimit)
        assertEquals(listOf(weekly), data.weeklyLimits)
        // primaryLimit prefers the active window.
        assertEquals(weekly, data.primaryLimit)
    }

    @Test
    fun primaryLimit_fallsBackToSessionThenFirst() {
        val session = limit(kind = "session", group = "session", isActive = false)
        val weekly = limit(kind = "weekly_all", group = "weekly", isActive = false)
        val data = UsageData(limits = listOf(weekly, session))
        // No active window -> falls back to the session window.
        assertEquals(session, data.primaryLimit)
    }

    private fun limit(
        kind: String = "session",
        group: String = "session",
        percent: Int = 0,
        isActive: Boolean = true
    ) = UsageLimit(
        kind = kind,
        group = group,
        percent = percent,
        severity = "normal",
        resetsAt = null,
        isActive = isActive
    )
}
