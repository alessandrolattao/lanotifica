package com.alessandrolattao.lanotifica.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alessandrolattao.lanotifica.R
import com.alessandrolattao.lanotifica.network.HealthMonitor
import com.alessandrolattao.lanotifica.service.NotificationForwarderService
import com.alessandrolattao.lanotifica.ui.components.QrScanner
import com.alessandrolattao.lanotifica.util.LocalNetworkPermission
import com.alessandrolattao.lanotifica.util.QrCodeParser
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory)) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val hasNotificationAccess by viewModel.hasNotificationAccess.collectAsStateWithLifecycle()
    val isBatteryOptimizationDisabled by
        viewModel.isBatteryOptimizationDisabled.collectAsStateWithLifecycle()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()
    val blacklistedAppsCount by viewModel.blacklistedAppsCount.collectAsStateWithLifecycle()
    val hasLocalNetworkPermission by
        viewModel.hasLocalNetworkPermission.collectAsStateWithLifecycle()
    val localNetworkRequested by viewModel.localNetworkRequested.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    var showQrScanner by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBlacklist by remember { mutableStateOf(false) }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            viewModel.setCameraPermission(isGranted)
            if (isGranted) {
                showQrScanner = true
            }
        }

    // Forwarding works without it, but the foreground service notification stays hidden
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Android 17 blocks discovery and the server connection until this is granted
    val localNetworkPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            viewModel.setLocalNetworkPermission(isGranted)
            if (isGranted) {
                cancelLocalNetworkNotification(context)
            }
        }

    // Check permissions when app resumes (lifecycle-aware, no polling!)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissions(
                    notificationAccess = isNotificationServiceEnabled(context),
                    batteryOptDisabled = isBatteryOptimizationDisabled(context),
                )
                val localNetworkGranted = !LocalNetworkPermission.isMissing(context)
                viewModel.setLocalNetworkPermission(localNetworkGranted)
                if (localNetworkGranted) {
                    cancelLocalNetworkNotification(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showBlacklist) {
        BlacklistScreen(onClose = { showBlacklist = false })
        return
    }

    if (showQrScanner) {
        QrScannerScreen(
            onQrCodeScanned = { qrData ->
                val parsed = QrCodeParser.parse(qrData)
                if (parsed != null) {
                    viewModel.setServerConfig(parsed.token, parsed.fingerprint)
                }
                showQrScanner = false
            },
            onClose = { showQrScanner = false },
        )
        return
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "LaNotifica",
                        modifier = Modifier.size(240.dp),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("LaNotifica")
                }
            },
            text = {
                Column {
                    Text(
                        "Forward notifications from your Android device to your Linux desktop.\n\n" +
                            "To get started, install and run the LaNotifica server on your computer. " +
                            "The website explains how.",
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Version ${appVersionName(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Made by Alessandro Lattao",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        openWebsite(context)
                    },
                ) {
                    Text("Website")
                }
            },
            dismissButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } },
        )
    }

    MainScreenContent(
        isConfigured = isConfigured,
        serviceEnabled = serviceEnabled,
        connectionState = connectionState,
        serverUrl = serverUrl,
        hasNotificationAccess = hasNotificationAccess,
        isBatteryOptimizationDisabled = isBatteryOptimizationDisabled,
        onNotificationAccessClick = { openNotificationListenerSettings(context) },
        onBatteryOptimizationClick = { requestDisableBatteryOptimization(context) },
        onServerConfigClick = {
            if (hasCameraPermission) {
                showQrScanner = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onServiceEnabledChange = { enabled ->
            if (enabled && !hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.setServiceEnabled(enabled)
        },
        onAboutClick = { showAboutDialog = true },
        onFilterClick = { showBlacklist = true },
        blacklistedAppsCount = blacklistedAppsCount,
        showLocalNetwork = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN,
        hasLocalNetworkPermission = hasLocalNetworkPermission,
        onLocalNetworkClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
                // After a permanent denial Android no longer shows the prompt
                if (
                    localNetworkRequested &&
                        activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                ) {
                    openAppSettings(context)
                } else {
                    viewModel.markLocalNetworkRequested()
                    localNetworkPermissionLauncher.launch(permission)
                }
            }
        },
    )
}

/** Stateless content composable for MainScreen. Extracted for testability. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    isConfigured: Boolean,
    serviceEnabled: Boolean,
    connectionState: HealthMonitor.ConnectionState,
    serverUrl: String?,
    hasNotificationAccess: Boolean,
    isBatteryOptimizationDisabled: Boolean,
    onNotificationAccessClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    onServerConfigClick: () -> Unit,
    onServiceEnabledChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onFilterClick: () -> Unit,
    blacklistedAppsCount: Int = 0,
    showLocalNetwork: Boolean = false,
    hasLocalNetworkPermission: Boolean = true,
    onLocalNetworkClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LaNotifica") },
                actions = {
                    IconButton(onClick = onAboutClick) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "About")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Setup section
            SectionHeader("Setup")

            // Notification Access
            SettingRow(
                icon =
                    if (hasNotificationAccess) Icons.Default.Notifications
                    else Icons.Default.NotificationsOff,
                title = "Notification Access",
                subtitle = if (hasNotificationAccess) "Granted" else "Tap to grant",
                isOk = hasNotificationAccess,
                onClick = onNotificationAccessClick,
            )

            // Local Network (Android 17+)
            if (showLocalNetwork) {
                SettingRow(
                    icon =
                        if (hasLocalNetworkPermission) Icons.Default.Wifi
                        else Icons.Default.WifiOff,
                    title = "Local Network",
                    subtitle = if (hasLocalNetworkPermission) "Allowed" else "Tap to allow",
                    isOk = hasLocalNetworkPermission,
                    onClick = onLocalNetworkClick,
                )
            }

            // Battery Optimization
            SettingRow(
                icon =
                    if (isBatteryOptimizationDisabled) Icons.Default.BatterySaver
                    else Icons.Default.BatteryAlert,
                title = "Battery Optimization",
                subtitle = if (isBatteryOptimizationDisabled) "Unrestricted" else "Tap to disable",
                isOk = isBatteryOptimizationDisabled,
                onClick = onBatteryOptimizationClick,
            )

            // QR Configuration
            Card(modifier = Modifier.fillMaxWidth(), onClick = onServerConfigClick) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Server Configuration",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text =
                                if (isConfigured) "Tap to reconfigure" else "Tap to scan QR code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector =
                            if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint =
                            if (isConfigured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    )
                }
            }

            // App Filters
            Card(modifier = Modifier.fillMaxWidth(), onClick = onFilterClick) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Filters",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text =
                                if (blacklistedAppsCount == 0) "All apps forwarded"
                                else
                                    "$blacklistedAppsCount app${if (blacklistedAppsCount == 1) "" else "s"} blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Status section
            SectionHeader("Status")

            // Server Connection Status
            SettingRow(
                icon =
                    when (connectionState) {
                        HealthMonitor.ConnectionState.CONNECTED -> Icons.Default.Cloud
                        HealthMonitor.ConnectionState.CONNECTING -> Icons.Default.Sync
                        HealthMonitor.ConnectionState.DISCONNECTED -> Icons.Default.CloudOff
                    },
                title = "Server Status",
                subtitle =
                    when {
                        !isConfigured -> "Configure first"
                        connectionState == HealthMonitor.ConnectionState.CONNECTED ->
                            serverUrl ?: "Connected"
                        connectionState == HealthMonitor.ConnectionState.CONNECTING ->
                            "Discovering..."
                        else -> "Not found"
                    },
                isOk = connectionState == HealthMonitor.ConnectionState.CONNECTED,
                onClick = null,
            )

            // Forward Switch
            val isForwardingActive =
                serviceEnabled &&
                    hasNotificationAccess &&
                    isConfigured &&
                    connectionState == HealthMonitor.ConnectionState.CONNECTED

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector =
                            if (isForwardingActive) Icons.Default.CheckCircle
                            else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint =
                            if (isForwardingActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Forwarding", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text =
                                when {
                                    !hasNotificationAccess -> "Grant access first"
                                    !isConfigured -> "Configure first"
                                    !serviceEnabled -> "Disabled"
                                    connectionState != HealthMonitor.ConnectionState.CONNECTED ->
                                        "Waiting for server..."
                                    else -> "Active"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = onServiceEnabledChange,
                        enabled = hasNotificationAccess && isConfigured,
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
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isOk: Boolean,
    onClick: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick ?: {}) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint =
                    if (isOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScannerScreen(onQrCodeScanned: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            QrScanner(onQrCodeScanned = onQrCodeScanned, modifier = Modifier.fillMaxSize())

            Card(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
                Text(
                    text = "Point camera at server QR code",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val componentName =
        ComponentName(
            context,
            "com.alessandrolattao.lanotifica.service.NotificationForwarderService",
        )
    val enabledListeners =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(componentName.flattenToString()) == true
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

private fun appVersionName(context: Context): String? =
    context.packageManager
        .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        .versionName

private fun cancelLocalNetworkNotification(context: Context) {
    NotificationManagerCompat.from(context)
        .cancel(NotificationForwarderService.LOCAL_NETWORK_NOTIFICATION_ID)
}

private fun hasNotificationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    context.startActivity(intent)
}

private fun requestDisableBatteryOptimization(context: Context) {
    val intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    context.startActivity(intent)
}

private fun openWebsite(context: Context) {
    val path =
        if (Locale.getDefault().language == "it") "/it/opensource/lanotifica.html"
        else "/opensource/lanotifica.html"
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            data =
                Uri.parse(
                    "https://alessandrolattao.com$path?utm_source=lanotifica&utm_medium=app",
                )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    context.startActivity(intent)
}
