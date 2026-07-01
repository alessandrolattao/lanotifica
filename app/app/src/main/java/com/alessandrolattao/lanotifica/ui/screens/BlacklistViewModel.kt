package com.alessandrolattao.lanotifica.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alessandrolattao.lanotifica.data.SettingsRepository
import com.alessandrolattao.lanotifica.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppEntry(val packageName: String, val appName: String, val isBlacklisted: Boolean)

class BlacklistViewModel(
    private val settingsRepository: SettingsRepository,
    private val packageManager: PackageManager,
) : ViewModel() {

    val apps: StateFlow<List<AppEntry>> =
        combine(settingsRepository.knownApps, settingsRepository.blacklistedApps) {
                known,
                blacklisted ->
                val uninstalled = mutableSetOf<String>()
                val entries =
                    known
                        .mapNotNull { pkg ->
                            try {
                                val info =
                                    packageManager.getApplicationInfo(
                                        pkg,
                                        PackageManager.ApplicationInfoFlags.of(0),
                                    )
                                val name = packageManager.getApplicationLabel(info).toString()
                                AppEntry(
                                    packageName = pkg,
                                    appName = name,
                                    isBlacklisted = pkg in blacklisted,
                                )
                            } catch (_: PackageManager.NameNotFoundException) {
                                uninstalled.add(pkg)
                                null
                            }
                        }
                        .sortedBy { it.appName.lowercase() }
                if (uninstalled.isNotEmpty()) {
                    uninstalled.forEach { settingsRepository.removeKnownApp(it) }
                }
                entries
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBlacklisted(packageName: String, blacklisted: Boolean) {
        viewModelScope.launch { settingsRepository.setAppBlacklisted(packageName, blacklisted) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BlacklistViewModel(
                        settingsRepository = AppModule.settingsRepository,
                        packageManager = context.packageManager,
                    )
                        as T
            }
    }
}
