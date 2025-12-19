package com.alessandrolattao.lanotifica.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alessandrolattao.lanotifica.LaNotificaApp
import com.alessandrolattao.lanotifica.MainActivity
import com.alessandrolattao.lanotifica.R
import com.alessandrolattao.lanotifica.di.AppModule
import com.alessandrolattao.lanotifica.network.ApiClient
import com.alessandrolattao.lanotifica.network.DismissRequest
import com.alessandrolattao.lanotifica.network.NotificationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationForwarderService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationForwarder"
        private const val FOREGROUND_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settingsRepository by lazy { AppModule.settingsRepository }
    private val healthMonitor by lazy { AppModule.healthMonitor }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        startForeground(
            FOREGROUND_ID,
            createForegroundNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        healthMonitor.startMonitoring()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener disconnected")
        healthMonitor.stopMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        healthMonitor.stopMonitoring()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName == packageName) return

        // Skip ongoing notifications (media players, etc.)
        if (sbn.isOngoing) return

        // Skip notifications that can't be dismissed by user (system noise like Now Playing)
        if (sbn.notification.flags and Notification.FLAG_NO_CLEAR != 0) return

        serviceScope.launch {
            try {
                val enabled = settingsRepository.serviceEnabled.first()
                if (!enabled) {
                    Log.d(TAG, "Service disabled, skipping notification")
                    return@launch
                }

                val authToken = settingsRepository.authToken.first()
                if (authToken.isBlank()) {
                    Log.d(TAG, "Auth token not configured, skipping notification")
                    return@launch
                }

                val certFingerprint = settingsRepository.certFingerprint.first()
                if (certFingerprint.isBlank()) {
                    Log.d(TAG, "Certificate fingerprint not configured, skipping notification")
                    return@launch
                }

                // Check if server is connected via HealthMonitor
                val serverUrl = healthMonitor.getServerUrlIfConnected()
                if (serverUrl == null) {
                    Log.d(TAG, "Server not connected, skipping notification")
                    return@launch
                }

                val notification = sbn.notification
                val extras = notification.extras

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                if (text.isBlank()) {
                    Log.d(TAG, "Empty notification text, skipping")
                    return@launch
                }

                val appName = getAppName(sbn.packageName)

                // Extract importance from notification channel
                val channelId = notification.channelId
                val importance = if (channelId != null) {
                    NotificationManagerCompat.from(this@NotificationForwarderService)
                        .getNotificationChannelCompat(channelId)?.importance
                        ?: NotificationManager.IMPORTANCE_DEFAULT
                } else {
                    NotificationManager.IMPORTANCE_DEFAULT
                }

                // Map Android importance (1-5) to D-Bus urgency (0-2)
                val urgency = when (importance) {
                    NotificationManager.IMPORTANCE_MIN,
                    NotificationManager.IMPORTANCE_LOW -> 0 // Low
                    NotificationManager.IMPORTANCE_DEFAULT -> 1 // Normal
                    NotificationManager.IMPORTANCE_HIGH,
                    NotificationManager.IMPORTANCE_MAX -> 2 // Critical
                    else -> 1 // Normal fallback
                }

                // Get timeout (0 means no timeout set, capped to Int.MAX_VALUE)
                val timeoutMs = notification.timeoutAfter.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

                Log.d(TAG, "Forwarding notification from $appName: $title - $text (urgency=$urgency, timeout=$timeoutMs)")

                val request =
                    NotificationRequest(
                        key = sbn.key,
                        app_name = appName,
                        package_name = sbn.packageName,
                        title = title,
                        message = text,
                        urgency = urgency,
                        timeout_ms = if (timeoutMs > 0) timeoutMs else -1,
                    )

                try {
                    val api = ApiClient.getApi(serverUrl, authToken, certFingerprint)
                    val response = api.sendNotification(request)

                    if (response.isSuccessful) {
                        Log.d(TAG, "Notification forwarded successfully")
                    } else {
                        Log.e(TAG, "Failed to forward notification: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName == packageName) return

        serviceScope.launch {
            try {
                val enabled = settingsRepository.serviceEnabled.first()
                if (!enabled) return@launch

                val authToken = settingsRepository.authToken.first()
                if (authToken.isBlank()) return@launch

                val certFingerprint = settingsRepository.certFingerprint.first()
                if (certFingerprint.isBlank()) return@launch

                val serverUrl = healthMonitor.getServerUrlIfConnected() ?: return@launch

                Log.d(TAG, "Dismissing notification: ${sbn.key}")

                try {
                    val api = ApiClient.getApi(serverUrl, authToken, certFingerprint)
                    val response = api.dismissNotification(DismissRequest(key = sbn.key))

                    if (response.isSuccessful) {
                        Log.d(TAG, "Notification dismissed successfully")
                    } else {
                        Log.e(TAG, "Failed to dismiss notification: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error dismissing notification: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing notification", e)
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(this, LaNotificaApp.CHANNEL_ID)
            .setContentTitle("LaNotifica")
            .setContentText("Forwarding notifications...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
