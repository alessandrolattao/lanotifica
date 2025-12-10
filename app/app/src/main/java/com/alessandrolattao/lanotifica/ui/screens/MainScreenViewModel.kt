package com.alessandrolattao.lanotifica.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alessandrolattao.lanotifica.data.SettingsRepository
import com.alessandrolattao.lanotifica.di.AppModule
import com.alessandrolattao.lanotifica.network.HealthMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val healthMonitor: HealthMonitor,
) : ViewModel() {

    val isConfigured =
        settingsRepository.isConfigured.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false,
        )

    val serviceEnabled =
        settingsRepository.serviceEnabled.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false,
        )

    val connectionState: StateFlow<HealthMonitor.ConnectionState> = healthMonitor.connectionState

    val serverUrl: StateFlow<String?> = healthMonitor.serverUrl

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()

    private val _isBatteryOptimizationDisabled = MutableStateFlow(false)
    val isBatteryOptimizationDisabled: StateFlow<Boolean> =
        _isBatteryOptimizationDisabled.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    fun updatePermissions(
        notificationAccess: Boolean,
        batteryOptDisabled: Boolean,
        cameraPermission: Boolean = _hasCameraPermission.value,
    ) {
        _hasNotificationAccess.value = notificationAccess
        _isBatteryOptimizationDisabled.value = batteryOptDisabled
        _hasCameraPermission.value = cameraPermission
    }

    fun setCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setServiceEnabled(enabled) }
    }

    fun setServerConfig(token: String, fingerprint: String) {
        viewModelScope.launch {
            settingsRepository.setServerConfig(token, fingerprint)
            healthMonitor.forceCheck()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainScreenViewModel(
                        settingsRepository = AppModule.settingsRepository,
                        healthMonitor = AppModule.healthMonitor,
                    )
                        as T
                }
            }
    }
}
