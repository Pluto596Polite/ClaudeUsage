package com.adriaan.claudeusage.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.adriaan.claudeusage.data.model.UsageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_COOKIES = stringPreferencesKey("cookies")
        val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
        val KEY_MESSAGES_USED = intPreferencesKey("messages_used")
        val KEY_MESSAGES_LIMIT = intPreferencesKey("messages_limit")
        val KEY_RESET_AT = stringPreferencesKey("reset_at")
        val KEY_PLAN_NAME = stringPreferencesKey("plan_name")
        val KEY_ORG_NAME = stringPreferencesKey("org_name")
        val KEY_LAST_FETCHED = longPreferencesKey("last_fetched")
        val KEY_RAW_JSON = stringPreferencesKey("raw_json")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOGGED_IN] ?: false }

    val cookies: Flow<String?> = context.dataStore.data.map { it[KEY_COOKIES] }

    val usageData: Flow<UsageData> = context.dataStore.data.map { prefs ->
        UsageData(
            messagesUsed = prefs[KEY_MESSAGES_USED] ?: 0,
            messagesLimit = prefs[KEY_MESSAGES_LIMIT] ?: 0,
            resetAtIso = prefs[KEY_RESET_AT],
            planName = prefs[KEY_PLAN_NAME],
            orgName = prefs[KEY_ORG_NAME],
            lastFetchedEpoch = prefs[KEY_LAST_FETCHED] ?: 0L,
            rawJson = prefs[KEY_RAW_JSON]
        )
    }

    suspend fun saveSession(cookies: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COOKIES] = cookies
            prefs[KEY_LOGGED_IN] = true
        }
    }

    suspend fun saveUsageData(data: UsageData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MESSAGES_USED] = data.messagesUsed
            prefs[KEY_MESSAGES_LIMIT] = data.messagesLimit
            data.resetAtIso?.let { prefs[KEY_RESET_AT] = it }
            data.planName?.let { prefs[KEY_PLAN_NAME] = it }
            data.orgName?.let { prefs[KEY_ORG_NAME] = it }
            prefs[KEY_LAST_FETCHED] = System.currentTimeMillis()
            data.rawJson?.let { prefs[KEY_RAW_JSON] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getCookies(): String? = cookies.first()
}
