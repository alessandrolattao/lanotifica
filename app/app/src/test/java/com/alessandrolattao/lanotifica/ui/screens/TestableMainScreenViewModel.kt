package com.alessandrolattao.lanotifica.ui.screens

import androidx.lifecycle.ViewModel
import com.alessandrolattao.lanotifica.network.HealthMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Testable version of MainScreenViewModel that accepts fake dependencies. Uses StateFlow directly
 * for simpler testing (no stateIn delays).
 */
class TestableMainScreenViewModel(
    isConfiguredFlow: StateFlow<Boolean>,
    serviceEnabledFlow: StateFlow<Boolean>,
    connectionStateFlow: StateFlow<HealthMonitor.ConnectionState>,
    serverUrlFlow: StateFlow<String?>,
) : ViewModel() {

    val isConfigured: StateFlow<Boolean> = isConfiguredFlow

    val serviceEnabled: StateFlow<Boolean> = serviceEnabledFlow

    val connectionState: StateFlow<HealthMonitor.ConnectionState> = connectionStateFlow

    val serverUrl: StateFlow<String?> = serverUrlFlow

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
}
