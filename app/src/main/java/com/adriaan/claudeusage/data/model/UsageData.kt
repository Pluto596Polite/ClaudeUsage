package com.adriaan.claudeusage.data.model

import com.google.gson.annotations.SerializedName

data class Organization(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("plan_tier") val planTier: String? = null,
    @SerializedName("billing_type") val billingType: String? = null,
    @SerializedName("capabilities") val capabilities: List<String>? = null
)

data class UsageResponse(
    @SerializedName("message_count") val messageCount: Int? = null,
    @SerializedName("message_limit") val messageLimit: Int? = null,
    @SerializedName("messages_used") val messagesUsed: Int? = null,
    @SerializedName("messages_limit") val messagesLimit: Int? = null,
    @SerializedName("next_reset_at") val nextResetAt: String? = null,
    @SerializedName("reset_at") val resetAt: String? = null,
    @SerializedName("period_start") val periodStart: String? = null,
    @SerializedName("period_end") val periodEnd: String? = null,
    @SerializedName("token_count") val tokenCount: Long? = null,
    @SerializedName("token_limit") val tokenLimit: Long? = null,
    @SerializedName("plan") val plan: String? = null,
    @SerializedName("tier") val tier: String? = null
)

data class UsageData(
    val messagesUsed: Int = 0,
    val messagesLimit: Int = 0,
    val resetAtIso: String? = null,
    val planName: String? = null,
    val orgName: String? = null,
    val lastFetchedEpoch: Long = 0L,
    val rawJson: String? = null
) {
    val percentUsed: Float
        get() = if (messagesLimit > 0) messagesUsed.toFloat() / messagesLimit.toFloat() else 0f

    val hasData: Boolean
        get() = messagesLimit > 0 || lastFetchedEpoch > 0

    companion object {
        val Empty = UsageData()
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
