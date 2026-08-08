package com.adriaan.claudeusage.data.local

/**
 * Pure parsing/normalisation for the comma-separated alert-threshold string persisted by
 * [SettingsManager]. Framework-free so it can be unit-tested.
 */
object Thresholds {

    const val MIN = 1
    const val MAX = 100

    val DEFAULT = listOf(50, 75, 90)

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)

    /**
     * Parse the stored value into thresholds: ascending, de-duplicated, each clamped to [MIN]..[MAX].
     * `null` (never set) yields [DEFAULT]; an explicitly blank string yields an empty list (the user
     * removed them all).
     */
    fun parse(raw: String?): List<Int> {
        if (raw == null) return DEFAULT
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { clamp(it) }
            .distinct()
            .sorted()
    }

    /** Serialise thresholds back to the stored comma-separated form. */
    fun serialize(values: Collection<Int>): String = values.sorted().joinToString(",")
}
