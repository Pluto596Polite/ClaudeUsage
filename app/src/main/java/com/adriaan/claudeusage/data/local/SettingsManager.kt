package com.adriaan.claudeusage.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Separate DataStore from the session store so clearing the session (logout) never wipes the
// user's alert preferences.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * User-configurable quota-alert settings.
 *
 * A threshold is a percent (e.g. 50, 75). When any usage window crosses one of the configured
 * thresholds a phone notification fires — once per window per threshold, tracked via "fired flags"
 * so the same alert is not repeated on every 15-minute background sync. A flag is cleared once the
 * window's percent falls back below the threshold (i.e. after the window resets), re-arming the alert.
 */
class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_THRESHOLDS = stringPreferencesKey("alert_thresholds")
        private val KEY_FIRED_FLAGS = stringPreferencesKey("alert_fired_flags")

        val DEFAULT_THRESHOLDS = Thresholds.DEFAULT
        const val MIN_THRESHOLD = Thresholds.MIN
        const val MAX_THRESHOLD = Thresholds.MAX
    }

    val notificationsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    /** Configured thresholds, ascending, de-duplicated. */
    val thresholds: Flow<List<Int>> =
        context.settingsDataStore.data.map { Thresholds.parse(it[KEY_THRESHOLDS]) }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun addThreshold(percent: Int) {
        val clamped = Thresholds.clamp(percent)
        context.settingsDataStore.edit { prefs ->
            val current = Thresholds.parse(prefs[KEY_THRESHOLDS]).toMutableSet()
            current.add(clamped)
            prefs[KEY_THRESHOLDS] = Thresholds.serialize(current)
        }
    }

    suspend fun removeThreshold(percent: Int) {
        context.settingsDataStore.edit { prefs ->
            val current = Thresholds.parse(prefs[KEY_THRESHOLDS]).toMutableSet()
            current.remove(percent)
            prefs[KEY_THRESHOLDS] = Thresholds.serialize(current)
        }
    }

    suspend fun getNotificationsEnabled(): Boolean = notificationsEnabled.first()

    suspend fun getThresholds(): List<Int> = thresholds.first()

    // --- Fired-flag bookkeeping (keyed "windowKind@threshold") -------------------------------

    suspend fun getFiredFlags(): Set<String> =
        context.settingsDataStore.data.map { parseFlags(it[KEY_FIRED_FLAGS]) }.first()

    /** Replace the persisted set of fired flags. */
    suspend fun setFiredFlags(flags: Set<String>) {
        context.settingsDataStore.edit { it[KEY_FIRED_FLAGS] = flags.joinToString(",") }
    }

    private fun parseFlags(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
