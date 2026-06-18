package com.adriaan.claudeusage.data.api

import com.adriaan.claudeusage.data.model.AccountInfo
import com.adriaan.claudeusage.data.model.Organization
import com.adriaan.claudeusage.data.model.UsageData
import com.adriaan.claudeusage.data.model.UsageLimit
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

    /** HTTP status code + body (body null on transport failure). status -1 = exception. */
    private data class HttpResult(val status: Int, val body: String?)

    private fun get(path: String): HttpResult {
        return try {
            val request = Request.Builder()
                .url("https://claude.ai$path")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            android.util.Log.d("ClaudeUsageNet", "GET $path -> ${response.code} (${body?.length ?: 0} bytes)")
            HttpResult(response.code, if (response.isSuccessful) body else null)
        } catch (e: Exception) {
            android.util.Log.w("ClaudeUsageNet", "GET $path failed: ${e.message}")
            HttpResult(-1, null)
        }
    }

    fun getOrganizations(): List<Organization> {
        val json = get("/api/organizations").body ?: return emptyList()
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

    /**
     * Result of a usage fetch. On success [limits] is non-empty and [raw] holds the JSON that
     * produced it. On failure [limits] is empty and [raw] holds a diagnostic of every endpoint
     * tried with its HTTP status — surfaced in the UI so a moved/auth-failed endpoint is visible
     * instead of silently showing zeros.
     */
    data class UsageResult(val limits: List<UsageLimit>, val raw: String?)

    fun getUsage(orgId: String): UsageResult {
        // claude.ai serves Pro usage from /usage as percent-per-window limit objects.
        // Keep fallbacks in case the path moves.
        val endpoints = listOf(
            "/api/organizations/$orgId/usage",
            "/api/organizations/$orgId/limits",
            "/api/account/usage",
            "/api/usage"
        )

        val diagnostics = StringBuilder("No usage endpoint returned usable data.\n\nTried:\n")
        for (endpoint in endpoints) {
            val result = get(endpoint)
            val statusLabel = if (result.status == -1) "network error" else "HTTP ${result.status}"
            diagnostics.append("• $endpoint → $statusLabel\n")

            val raw = result.body ?: continue
            val limits = UsageData.parseLimits(raw)
            if (limits.isNotEmpty()) return UsageResult(limits, raw)
        }
        return UsageResult(emptyList(), diagnostics.toString())
    }

    fun getAccountInfo(): AccountInfo? {
        val endpoints = listOf("/api/auth/session", "/api/account", "/api/me")
        for (endpoint in endpoints) {
            val raw = get(endpoint).body ?: continue
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
