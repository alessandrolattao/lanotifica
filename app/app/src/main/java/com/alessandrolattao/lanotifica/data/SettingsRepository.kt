package com.alessandrolattao.lanotifica.data

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "SettingsRepository"
        private const val KEYSET_NAME = "lanotifica_keyset"
        private const val PREF_FILE_NAME = "lanotifica_keyset_prefs"
        private const val MASTER_KEY_ALIAS = "lanotifica_master_key"
        private const val MASTER_KEY_URI = "android-keystore://$MASTER_KEY_ALIAS"

        // Keys for DataStore
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val CERT_FINGERPRINT = stringPreferencesKey("cert_fingerprint")
        private val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val CACHED_SERVER_URL = stringPreferencesKey("cached_server_url")
        private val KNOWN_APPS = stringSetPreferencesKey("known_apps")
        private val BLACKLISTED_APPS = stringSetPreferencesKey("blacklisted_apps")
    }

    private val aead: Aead by lazy {
        AeadConfig.register()
        try {
            createAead()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load existing keys, recreating: ${e.message}")
            resetEncryptionKeys()
            createAead()
        }
    }

    private fun createAead(): Aead {
        val keysetHandle =
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun resetEncryptionKeys() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                Log.i(TAG, "Deleted corrupted master key from KeyStore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete master key: ${e.message}")
        }

        try {
            context
                .getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            Log.i(TAG, "Cleared keyset preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear keyset prefs: ${e.message}")
        }
    }

    private fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        return try {
            val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
            String(aead.decrypt(decoded, null), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    val authToken: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[AUTH_TOKEN]?.let { decrypt(it) } ?: "" }

    val certFingerprint: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[CERT_FINGERPRINT]?.let { decrypt(it) } ?: "" }

    val serviceEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[SERVICE_ENABLED] ?: false }

    val cachedServerUrl: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[CACHED_SERVER_URL] ?: "" }

    val isConfigured: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            val token = prefs[AUTH_TOKEN]?.let { decrypt(it) } ?: ""
            val fingerprint = prefs[CERT_FINGERPRINT]?.let { decrypt(it) } ?: ""
            token.isNotBlank() && fingerprint.isNotBlank()
        }

    suspend fun setServerConfig(token: String, fingerprint: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = encrypt(token)
            prefs[CERT_FINGERPRINT] = encrypt(fingerprint)
            prefs.remove(CACHED_SERVER_URL)
        }
    }

    suspend fun setCachedServerUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[CACHED_SERVER_URL] = url }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SERVICE_ENABLED] = enabled }
    }

    val knownApps: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[KNOWN_APPS] ?: emptySet() }

    val blacklistedApps: Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[BLACKLISTED_APPS] ?: emptySet() }

    suspend fun addKnownApp(packageName: String) {
        val current = context.dataStore.data.first()[KNOWN_APPS] ?: emptySet()
        if (packageName in current) return
        context.dataStore.edit { prefs ->
            val stored = prefs[KNOWN_APPS] ?: emptySet()
            if (packageName !in stored) {
                prefs[KNOWN_APPS] = stored + packageName
            }
        }
    }

    suspend fun removeKnownApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KNOWN_APPS] ?: return@edit
            prefs[KNOWN_APPS] = current - packageName
            val blacklisted = prefs[BLACKLISTED_APPS] ?: return@edit
            prefs[BLACKLISTED_APPS] = blacklisted - packageName
        }
    }

    suspend fun setAppBlacklisted(packageName: String, blacklisted: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLACKLISTED_APPS] ?: emptySet()
            prefs[BLACKLISTED_APPS] =
                if (blacklisted) current + packageName else current - packageName
        }
    }
}
