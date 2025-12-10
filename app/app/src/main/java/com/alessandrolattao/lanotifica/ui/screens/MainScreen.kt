package com.alessandrolattao.lanotifica.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.alessandrolattao.lanotifica.R
import androidx.compose.ui.unit.dp
import com.alessandrolattao.lanotifica.data.SettingsRepository
import com.alessandrolattao.lanotifica.network.HealthMonitor
import com.alessandrolattao.lanotifica.ui.components.QrScanner
import com.alessandrolattao.lanotifica.util.QrCodeParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val healthMonitor = remember { HealthMonitor.getInstance(context) }

    val isConfigured by settingsRepository.isConfigured.collectAsState(initial = false)
    val serviceEnabled by settingsRepository.serviceEnabled.collectAsState(initial = false)
    val connectionState by healthMonitor.connectionState.collectAsState()
    val serverUrl by healthMonitor.serverUrl.collectAsState()

    var showQrScanner by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember { mutableStateOf(false) }
    var isBatteryOptimizationDisabled by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showQrScanner = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            hasNotificationAccess = isNotificationServiceEnabled(context)
            isBatteryOptimizationDisabled = isBatteryOptimizationDisabled(context)
            delay(1000)
        }
    }

    if (showQrScanner) {
        QrScannerScreen(
            onQrCodeScanned = { qrData ->
                val parsed = QrCodeParser.parse(qrData)
                if (parsed != null) {
                    scope.launch {
                        settingsRepository.setServerConfig(parsed.token, parsed.fingerprint)
                    }
                }
                showQrScanner = false
            },
            onClose = { showQrScanner = false }
        )
        return
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "LaNotifica",
                        modifier = Modifier.size(240.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("LaNotifica")
                }
            },
            text = {
                Text(
                    "Forward notifications from your Android device to your Linux desktop.\n\n" +
                    "To get started, you need to install and run the LaNotifica server on your computer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAboutDialog = false
                    openGitHub(context)
                }) {
                    Text("Setup Instructions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LaNotifica") },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = "About"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Setup section
            SectionHeader("Setup")

            // Notification Access
            SettingRow(
                icon = if (hasNotificationAccess) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                title = "Notification Access",
                subtitle = if (hasNotificationAccess) "Granted" else "Tap to grant",
                isOk = hasNotificationAccess,
                onClick = { openNotificationListenerSettings(context) }
            )

            // Battery Optimization
            SettingRow(
                icon = if (isBatteryOptimizationDisabled) Icons.Default.BatterySaver else Icons.Default.BatteryAlert,
                title = "Battery Optimization",
                subtitle = if (isBatteryOptimizationDisabled) "Unrestricted" else "Tap to disable",
                isOk = isBatteryOptimizationDisabled,
                onClick = { requestDisableBatteryOptimization(context) }
            )

            // QR Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (hasCameraPermission) {
                        showQrScanner = true
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Server Configuration",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isConfigured) "Tap to reconfigure" else "Tap to scan QR code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isConfigured)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            // Status section
            SectionHeader("Status")

            // Server Connection Status
            SettingRow(
                icon = when (connectionState) {
                    HealthMonitor.ConnectionState.CONNECTED -> Icons.Default.Cloud
                    HealthMonitor.ConnectionState.CONNECTING -> Icons.Default.Sync
                    HealthMonitor.ConnectionState.DISCONNECTED -> Icons.Default.CloudOff
                },
                title = "Server Status",
                subtitle = when {
                    !isConfigured -> "Configure first"
                    connectionState == HealthMonitor.ConnectionState.CONNECTED -> serverUrl ?: "Connected"
                    connectionState == HealthMonitor.ConnectionState.CONNECTING -> "Discovering..."
                    else -> "Not found"
                },
                isOk = connectionState == HealthMonitor.ConnectionState.CONNECTED,
                onClick = null
            )

            // Forward Switch
            val isForwardingActive = serviceEnabled && hasNotificationAccess && isConfigured &&
                connectionState == HealthMonitor.ConnectionState.CONNECTED

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isForwardingActive) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isForwardingActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Forwarding",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when {
                                !hasNotificationAccess -> "Grant access first"
                                !isConfigured -> "Configure first"
                                !serviceEnabled -> "Disabled"
                                connectionState != HealthMonitor.ConnectionState.CONNECTED -> "Waiting for server..."
                                else -> "Active"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsRepository.setServiceEnabled(enabled)
                            }
                        },
                        enabled = hasNotificationAccess && isConfigured
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isOk: Boolean,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isOk)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QrScanner(
                onQrCodeScanned = onQrCodeScanned,
                modifier = Modifier.fillMaxSize()
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text(
                    text = "Point camera at server QR code",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val componentName = ComponentName(context, "com.alessandrolattao.lanotifica.service.NotificationForwarderService")
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabledListeners?.contains(componentName.flattenToString()) == true
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

private fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestDisableBatteryOptimization(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://github.com/alessandrolattao/lanotifica")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
