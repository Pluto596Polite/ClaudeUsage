package com.adriaan.claudeusage.data.api

import com.adriaan.claudeusage.data.model.AccountInfo
import com.adriaan.claudeusage.data.model.Organization
import com.adriaan.claudeusage.data.model.UsageResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClaudeApiService(private val cookieString: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Cookie", cookieString)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://claude.ai/")
                .addHeader("Origin", "https://claude.ai")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun get(path: String): String? {
        return try {
            val request = Request.Builder()
                .url("https://claude.ai$path")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            null
        }
    }

    fun getOrganizations(): List<Organization> {
        val json = get("/api/organizations") ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Organization(
                    uuid = obj.optString("uuid"),
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    planTier = obj.optString("plan_tier").ifEmpty { null },
                    billingType = obj.optString("billing_type").ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUsage(orgId: String): Pair<UsageResponse?, String?> {
        // Try multiple endpoint patterns
        val endpoints = listOf(
            "/api/organizations/$orgId/usage",
            "/api/organizations/$orgId/subscription",
            "/api/organizations/$orgId/limits",
            "/api/account/usage",
            "/api/usage"
        )

        for (endpoint in endpoints) {
            val raw = get(endpoint) ?: continue
            if (raw.startsWith("{") || raw.startsWith("[")) {
                val usage = parseUsageJson(raw)
                if (usage != null) return Pair(usage, raw)
            }
        }
        return Pair(null, null)
    }

    private fun parseUsageJson(json: String): UsageResponse? {
        return try {
            val obj = if (json.startsWith("[")) {
                JSONArray(json).optJSONObject(0) ?: return null
            } else {
                JSONObject(json)
            }

            UsageResponse(
                messageCount = obj.optInt("message_count").takeIf { it > 0 },
                messageLimit = obj.optInt("message_limit").takeIf { it > 0 },
                messagesUsed = obj.optInt("messages_used").takeIf { it > 0 },
                messagesLimit = obj.optInt("messages_limit").takeIf { it > 0 },
                nextResetAt = obj.optString("next_reset_at").ifEmpty { null },
                resetAt = obj.optString("reset_at").ifEmpty { null },
                periodStart = obj.optString("period_start").ifEmpty { null },
                periodEnd = obj.optString("period_end").ifEmpty { null },
                plan = obj.optString("plan").ifEmpty { null },
                tier = obj.optString("tier").ifEmpty { null }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAccountInfo(): AccountInfo? {
        val endpoints = listOf("/api/auth/session", "/api/account", "/api/me")
        for (endpoint in endpoints) {
            val raw = get(endpoint) ?: continue
            try {
                val obj = JSONObject(raw)
                val user = obj.optJSONObject("user") ?: obj
                return AccountInfo(
                    id = user.optString("id"),
                    uuid = user.optString("uuid"),
                    email = user.optString("email"),
                    name = user.optString("name").ifEmpty { null },
                    displayName = user.optString("display_name").ifEmpty { null }
                )
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
}
