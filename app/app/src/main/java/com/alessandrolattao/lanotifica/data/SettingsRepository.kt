package com.alessandrolattao.lanotifica.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // Keys for encrypted storage (sensitive data)
        private const val ENCRYPTED_PREFS_NAME = "secure_settings"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CERT_FINGERPRINT = "cert_fingerprint"

        // Keys for DataStore (non-sensitive data)
        private val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val CACHED_SERVER_URL = stringPreferencesKey("cached_server_url")
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val authToken: Flow<String> =
        flow { emit(encryptedPrefs.getString(KEY_AUTH_TOKEN, "") ?: "") }.flowOn(Dispatchers.IO)

    val certFingerprint: Flow<String> =
        flow { emit(encryptedPrefs.getString(KEY_CERT_FINGERPRINT, "") ?: "") }
            .flowOn(Dispatchers.IO)

    val serviceEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[SERVICE_ENABLED] ?: false }

    val cachedServerUrl: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[CACHED_SERVER_URL] ?: "" }

    val isConfigured: Flow<Boolean> =
        flow {
                val token = encryptedPrefs.getString(KEY_AUTH_TOKEN, "") ?: ""
                val fingerprint = encryptedPrefs.getString(KEY_CERT_FINGERPRINT, "") ?: ""
                emit(token.isNotBlank() && fingerprint.isNotBlank())
            }
            .flowOn(Dispatchers.IO)

    suspend fun setServerConfig(token: String, fingerprint: String) {
        withContext(Dispatchers.IO) {
            encryptedPrefs
                .edit()
                .putString(KEY_AUTH_TOKEN, token)
                .putString(KEY_CERT_FINGERPRINT, fingerprint)
                .apply()
        }
        // Clear cached URL to force rediscovery
        context.dataStore.edit { preferences -> preferences.remove(CACHED_SERVER_URL) }
    }

    suspend fun setCachedServerUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[CACHED_SERVER_URL] = url }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[SERVICE_ENABLED] = enabled }
    }
}
