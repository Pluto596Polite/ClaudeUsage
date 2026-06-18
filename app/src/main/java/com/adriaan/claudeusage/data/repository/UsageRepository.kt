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

        val (usageResponse, rawJson) = api.getUsage(orgId)

        val data = UsageData(
            messagesUsed = usageResponse?.messageCount
                ?: usageResponse?.messagesUsed
                ?: 0,
            messagesLimit = usageResponse?.messageLimit
                ?: usageResponse?.messagesLimit
                ?: 0,
            resetAtIso = usageResponse?.nextResetAt
                ?: usageResponse?.resetAt
                ?: usageResponse?.periodEnd,
            planName = usageResponse?.plan
                ?: usageResponse?.tier
                ?: org.planTier
                ?: org.billingType,
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
