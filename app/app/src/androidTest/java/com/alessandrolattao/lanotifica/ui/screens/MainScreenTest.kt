package com.alessandrolattao.lanotifica.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alessandrolattao.lanotifica.network.HealthMonitor
import com.alessandrolattao.lanotifica.ui.theme.LaNotificaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysSetupSection() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = false,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.DISCONNECTED,
                    serverUrl = null,
                    hasNotificationAccess = false,
                    isBatteryOptimizationDisabled = false,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Setup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notification Access").assertIsDisplayed()
        composeTestRule.onNodeWithText("Battery Optimization").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server Configuration").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysStatusSection() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = false,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.DISCONNECTED,
                    serverUrl = null,
                    hasNotificationAccess = false,
                    isBatteryOptimizationDisabled = false,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forwarding").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsNotConfiguredStatus_whenNotConfigured() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = false,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.DISCONNECTED,
                    serverUrl = null,
                    hasNotificationAccess = true,
                    isBatteryOptimizationDisabled = true,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Configure first").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsConnectedStatus_whenConnected() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = true,
                    serviceEnabled = true,
                    connectionState = HealthMonitor.ConnectionState.CONNECTED,
                    serverUrl = "https://192.168.1.100:8443",
                    hasNotificationAccess = true,
                    isBatteryOptimizationDisabled = true,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("https://192.168.1.100:8443").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsDiscoveringStatus_whenConnecting() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = true,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.CONNECTING,
                    serverUrl = null,
                    hasNotificationAccess = true,
                    isBatteryOptimizationDisabled = true,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Discovering...").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsGrantedStatus_whenNotificationAccessGranted() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = false,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.DISCONNECTED,
                    serverUrl = null,
                    hasNotificationAccess = true,
                    isBatteryOptimizationDisabled = false,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Granted").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsTapToGrantStatus_whenNotificationAccessNotGranted() {
        composeTestRule.setContent {
            LaNotificaTheme {
                MainScreenContent(
                    isConfigured = false,
                    serviceEnabled = false,
                    connectionState = HealthMonitor.ConnectionState.DISCONNECTED,
                    serverUrl = null,
                    hasNotificationAccess = false,
                    isBatteryOptimizationDisabled = false,
                    hasCameraPermission = false,
                    onNotificationAccessClick = {},
                    onBatteryOptimizationClick = {},
                    onServerConfigClick = {},
                    onServiceEnabledChange = {},
                    onAboutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Tap to grant").assertIsDisplayed()
    }
}
