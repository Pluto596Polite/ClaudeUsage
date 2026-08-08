package com.adriaan.claudeusage.data.update

/**
 * Pure version-string helpers used by the auto-updater. Kept framework-free so the comparison
 * logic can be unit-tested without Android or the network.
 */
object VersionCompare {

    /** Strip a leading "v"/"V" and any pre-release/build suffix, keeping the dotted numeric core. */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }
    }

    /**
     * True when [candidate] is a strictly higher dotted-numeric version than [current].
     * Missing components are treated as 0, so "1.1" == "1.1.0" and "1.2" > "1.1.9".
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val b = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai != bi) return ai > bi
        }
        return false
    }
}
