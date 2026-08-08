package com.adriaan.claudeusage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adriaan.claudeusage.data.model.UsageData
import com.adriaan.claudeusage.data.repository.UsageRepository
import com.adriaan.claudeusage.data.update.UpdateChecker
import com.adriaan.claudeusage.data.update.UpdateInfo
import com.adriaan.claudeusage.notification.QuotaAlertNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: UsageData) : UiState()
    data class Error(val message: String, val cachedData: UsageData? = null) : UiState()
}

sealed class NavEvent {
    data object NavigateToDashboard : NavEvent()
    data object NavigateToLogin : NavEvent()
}

class MainViewModel(
    private val repository: UsageRepository,
    private val quotaAlertNotifier: QuotaAlertNotifier? = null,
    private val updateChecker: UpdateChecker = UpdateChecker()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _navEvents = MutableSharedFlow<NavEvent>()
    val navEvents: SharedFlow<NavEvent> = _navEvents.asSharedFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private var updateCheckDone = false

    fun loadCachedData() {
        viewModelScope.launch {
            val cached = repository.getStoredUsage()
            if (cached.hasData) {
                _uiState.value = UiState.Success(cached)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val cached = (_uiState.value as? UiState.Success)?.data

            repository.fetchUsageData()
                .onSuccess { data ->
                    _uiState.value = UiState.Success(data)
                    quotaAlertNotifier?.evaluate(data)
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        message = e.message ?: "Unknown error",
                        cachedData = cached
                    )
                }

            _isRefreshing.value = false
        }
    }

    fun onLoginSuccess(cookies: String) {
        viewModelScope.launch {
            repository.saveSession(cookies)
            _navEvents.emit(NavEvent.NavigateToDashboard)
            _uiState.value = UiState.Loading
            refresh()
        }
    }

    /** Check GitHub Releases for a newer build. Runs at most once per app session. */
    fun checkForUpdate() {
        if (updateCheckDone) return
        updateCheckDone = true
        viewModelScope.launch {
            updateChecker.check().onSuccess { info ->
                _updateInfo.value = info
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = UiState.Loading
            _navEvents.emit(NavEvent.NavigateToLogin)
        }
    }
}
