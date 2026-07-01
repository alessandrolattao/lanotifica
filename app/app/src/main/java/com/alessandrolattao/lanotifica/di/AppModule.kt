package com.alessandrolattao.lanotifica.di

import android.content.Context
import com.alessandrolattao.lanotifica.data.SettingsRepository
import com.alessandrolattao.lanotifica.network.HealthMonitor

/**
 * Simple manual dependency injection container. Provides singleton instances of app-wide
 * dependencies.
 */
object AppModule {
    private var _settingsRepository: SettingsRepository? = null
    private var _healthMonitor: HealthMonitor? = null

    /** Initializes the DI container. Must be called in Application.onCreate(). */
    fun init(context: Context) {
        val appContext = context.applicationContext
        _settingsRepository = SettingsRepository(appContext)
        _healthMonitor = HealthMonitor.getInstance(appContext)
    }

    val settingsRepository: SettingsRepository
        get() =
            checkNotNull(_settingsRepository) {
                "AppModule not initialized. Call init() in Application.onCreate()"
            }

    val healthMonitor: HealthMonitor
        get() =
            checkNotNull(_healthMonitor) {
                "AppModule not initialized. Call init() in Application.onCreate()"
            }
}
