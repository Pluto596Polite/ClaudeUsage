package com.adriaan.claudeusage.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun normalize_stripsLeadingVAndSuffix() {
        assertEquals("1.2.3", VersionCompare.normalize("v1.2.3"))
        assertEquals("1.2.3", VersionCompare.normalize("V1.2.3"))
        assertEquals("1.2.0", VersionCompare.normalize("v1.2.0-beta"))
        assertEquals("1.1.0", VersionCompare.normalize("1.1.0+build.7"))
        assertEquals("2", VersionCompare.normalize("v2"))
    }

    @Test
    fun normalize_blankOrNull_isEmpty() {
        assertEquals("", VersionCompare.normalize(null))
        assertEquals("", VersionCompare.normalize(""))
        assertEquals("", VersionCompare.normalize("   "))
    }

    @Test
    fun isNewer_detectsHigherVersion() {
        assertTrue(VersionCompare.isNewer("1.1.1", "1.1.0"))
        assertTrue(VersionCompare.isNewer("1.2.0", "1.1.9"))
        assertTrue(VersionCompare.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun isNewer_equalVersions_isFalse() {
        assertFalse(VersionCompare.isNewer("1.1.0", "1.1.0"))
        // Missing components are treated as zero.
        assertFalse(VersionCompare.isNewer("1.1", "1.1.0"))
        assertFalse(VersionCompare.isNewer("1.1.0", "1.1"))
    }

    @Test
    fun isNewer_olderVersion_isFalse() {
        assertFalse(VersionCompare.isNewer("1.0.9", "1.1.0"))
        assertFalse(VersionCompare.isNewer("1.1.0", "2.0.0"))
    }

    @Test
    fun isNewer_worksAfterNormalize_endToEnd() {
        val latest = VersionCompare.normalize("v1.2.0")
        val installed = VersionCompare.normalize("1.1.0")
        assertTrue(VersionCompare.isNewer(latest, installed))
    }
}
