package com.adriaan.claudeusage.data.repository

import com.adriaan.claudeusage.data.api.ClaudeApiService
import com.adriaan.claudeusage.data.local.SessionManager
import com.adriaan.claudeusage.data.model.UsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class UsageRepository(val sessionManager: SessionManager) {

    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    suspend fun fetchUsageData(): Result<UsageData> = withContext(Dispatchers.IO) {
        val cookies = sessionManager.getCookies()
            ?: return@withContext Result.failure(Exception("Not logged in"))

        val api = ClaudeApiService(cookies)

        val orgs = api.getOrganizations()
        if (orgs.isEmpty()) {
            return@withContext Result.failure(
                Exception("No organizations found — session may have expired.")
            )
        }

        val org = orgs.first()
        val orgId = org.uuid.ifEmpty { org.id }

        val (limits, rawJson) = api.getUsage(orgId)

        // No endpoint returned usable limit windows. Do NOT persist an empty result over the
        // last-known-good data — that would blank the dashboard and widget. Fail so the UI shows
        // the error (with cached data preserved) and the diagnostic of what was tried.
        if (limits.isEmpty()) {
            return@withContext Result.failure(
                Exception(rawJson ?: "Claude returned no usage data.")
            )
        }

        val data = UsageData(
            limits = limits,
            planName = org.planTier ?: org.billingType,
            orgName = org.name,
            lastFetchedEpoch = System.currentTimeMillis(),
            rawJson = rawJson
        )

        sessionManager.saveUsageData(data)
        Result.success(data)
    }

    suspend fun getStoredUsage(): UsageData = sessionManager.usageData.first()

    suspend fun isLoggedInNow(): Boolean = sessionManager.isLoggedIn.first()

    suspend fun saveSession(cookies: String) = sessionManager.saveSession(cookies)

    suspend fun logout() = sessionManager.clearSession()
}
