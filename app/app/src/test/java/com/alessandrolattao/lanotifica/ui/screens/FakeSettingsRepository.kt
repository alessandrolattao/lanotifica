package com.alessandrolattao.lanotifica.ui.screens

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Fake SettingsRepository for testing purposes. */
class FakeSettingsRepository {
    private val _authToken = MutableStateFlow("")
    val authToken: Flow<String> = _authToken

    private val _certFingerprint = MutableStateFlow("")
    val certFingerprint: Flow<String> = _certFingerprint

    private val _serviceEnabled = MutableStateFlow(false)
    val serviceEnabled: Flow<Boolean> = _serviceEnabled

    private val _cachedServerUrl = MutableStateFlow("")
    val cachedServerUrl: Flow<String> = _cachedServerUrl

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: Flow<Boolean> = _isConfigured

    suspend fun setServerConfig(token: String, fingerprint: String) {
        _authToken.value = token
        _certFingerprint.value = fingerprint
        _isConfigured.value = token.isNotBlank() && fingerprint.isNotBlank()
    }

    suspend fun setCachedServerUrl(url: String) {
        _cachedServerUrl.value = url
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        _serviceEnabled.value = enabled
    }
}
