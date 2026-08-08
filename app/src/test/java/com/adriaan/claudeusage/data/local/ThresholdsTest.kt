package com.adriaan.claudeusage.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ThresholdsTest {

    @Test
    fun parse_null_returnsDefaults() {
        assertEquals(Thresholds.DEFAULT, Thresholds.parse(null))
    }

    @Test
    fun parse_blank_returnsEmpty() {
        assertEquals(emptyList<Int>(), Thresholds.parse(""))
        assertEquals(emptyList<Int>(), Thresholds.parse("   "))
    }

    @Test
    fun parse_sortsDeduplicatesAndClamps() {
        assertEquals(listOf(25, 50, 90), Thresholds.parse("90,25,50,50"))
        // Out-of-range values are clamped into 1..100.
        assertEquals(listOf(1, 100), Thresholds.parse("0,150"))
    }

    @Test
    fun parse_ignoresNonNumericTokens() {
        assertEquals(listOf(50, 75), Thresholds.parse("50, abc, 75, "))
    }

    @Test
    fun clamp_boundsToRange() {
        assertEquals(Thresholds.MIN, Thresholds.clamp(0))
        assertEquals(Thresholds.MIN, Thresholds.clamp(-10))
        assertEquals(Thresholds.MAX, Thresholds.clamp(101))
        assertEquals(50, Thresholds.clamp(50))
    }

    @Test
    fun serialize_isSortedCsv() {
        assertEquals("25,50,90", Thresholds.serialize(listOf(90, 25, 50)))
    }

    @Test
    fun parse_thenSerialize_roundTrips() {
        val parsed = Thresholds.parse("75,50,90")
        assertEquals("50,75,90", Thresholds.serialize(parsed))
    }
}
