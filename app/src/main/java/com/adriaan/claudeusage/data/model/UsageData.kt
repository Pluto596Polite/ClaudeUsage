package com.adriaan.claudeusage.data.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class Organization(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("plan_tier") val planTier: String? = null,
    @SerializedName("billing_type") val billingType: String? = null,
    @SerializedName("capabilities") val capabilities: List<String>? = null
)

/**
 * One rate-limit window from claude.ai's `/api/organizations/{uuid}/usage` response.
 * claude.ai expresses Pro usage as a percent-consumed per window (a rolling "session"
 * cap plus longer "weekly" caps) — there is no raw message count.
 */
data class UsageLimit(
    val kind: String,        // "session", "weekly_all", "weekly_opus", ...
    val group: String,       // "session" | "weekly"
    val percent: Int,        // 0..100 of the cap consumed
    val severity: String,    // "normal" | "warning" | ...
    val resetsAt: String?,   // ISO-8601, when this window resets
    val isActive: Boolean
) {
    /** Consumed fraction, clamped to 0..1 for progress bars. */
    val fraction: Float get() = (percent / 100f).coerceIn(0f, 1f)
}

data class UsageData(
    val limits: List<UsageLimit> = emptyList(),
    val planName: String? = null,
    val orgName: String? = null,
    val lastFetchedEpoch: Long = 0L,
    val rawJson: String? = null
) {
    val sessionLimit: UsageLimit? get() = limits.firstOrNull { it.group == "session" }
    val weeklyLimits: List<UsageLimit> get() = limits.filter { it.group == "weekly" }

    /** The window worth featuring: the active one, else the session window, else the first. */
    val primaryLimit: UsageLimit?
        get() = limits.firstOrNull { it.isActive } ?: sessionLimit ?: limits.firstOrNull()

    val hasData: Boolean
        get() = limits.isNotEmpty() || lastFetchedEpoch > 0

    companion object {
        val Empty = UsageData()

        /**
         * Parse the `/usage` JSON body (`{"limits":[...]}`) into limit windows.
         * Returns an empty list on any failure or unexpected shape.
         */
        fun parseLimits(json: String?): List<UsageLimit> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONObject(json).optJSONArray("limits") ?: return emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    UsageLimit(
                        kind = o.optString("kind"),
                        group = o.optString("group"),
                        percent = o.optInt("percent"),
                        severity = o.optString("severity").ifEmpty { "normal" },
                        resetsAt = o.optString("resets_at").ifEmpty { null },
                        isActive = o.optBoolean("is_active")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class AccountInfo(
    @SerializedName("id") val id: String = "",
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("phone") val phone: String? = null
)
