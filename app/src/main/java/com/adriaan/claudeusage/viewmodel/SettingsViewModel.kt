package com.adriaan.claudeusage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adriaan.claudeusage.data.local.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val thresholds: StateFlow<List<Int>> = settingsManager.thresholds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsManager.DEFAULT_THRESHOLDS)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setNotificationsEnabled(enabled) }
    }

    fun addThreshold(percent: Int) {
        viewModelScope.launch { settingsManager.addThreshold(percent) }
    }

    fun removeThreshold(percent: Int) {
        viewModelScope.launch { settingsManager.removeThreshold(percent) }
    }
}
