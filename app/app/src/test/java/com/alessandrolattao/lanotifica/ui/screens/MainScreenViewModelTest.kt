package com.alessandrolattao.lanotifica.ui.screens

import com.alessandrolattao.lanotifica.network.HealthMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial permission states are false`() = runTest {
        val viewModel = createTestViewModel()

        assertFalse(viewModel.hasNotificationAccess.value)
        assertFalse(viewModel.isBatteryOptimizationDisabled.value)
        assertFalse(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `updatePermissions updates notification access state`() = runTest {
        val viewModel = createTestViewModel()

        viewModel.updatePermissions(notificationAccess = true, batteryOptDisabled = false)

        assertTrue(viewModel.hasNotificationAccess.value)
        assertFalse(viewModel.isBatteryOptimizationDisabled.value)
    }

    @Test
    fun `updatePermissions updates battery optimization state`() = runTest {
        val viewModel = createTestViewModel()

        viewModel.updatePermissions(notificationAccess = false, batteryOptDisabled = true)

        assertFalse(viewModel.hasNotificationAccess.value)
        assertTrue(viewModel.isBatteryOptimizationDisabled.value)
    }

    @Test
    fun `updatePermissions updates both states`() = runTest {
        val viewModel = createTestViewModel()

        viewModel.updatePermissions(notificationAccess = true, batteryOptDisabled = true)

        assertTrue(viewModel.hasNotificationAccess.value)
        assertTrue(viewModel.isBatteryOptimizationDisabled.value)
    }

    @Test
    fun `updatePermissions with camera permission updates all three states`() = runTest {
        val viewModel = createTestViewModel()

        viewModel.updatePermissions(
            notificationAccess = true,
            batteryOptDisabled = true,
            cameraPermission = true,
        )

        assertTrue(viewModel.hasNotificationAccess.value)
        assertTrue(viewModel.isBatteryOptimizationDisabled.value)
        assertTrue(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `setCameraPermission updates camera permission state`() = runTest {
        val viewModel = createTestViewModel()

        assertFalse(viewModel.hasCameraPermission.value)

        viewModel.setCameraPermission(true)
        assertTrue(viewModel.hasCameraPermission.value)

        viewModel.setCameraPermission(false)
        assertFalse(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `camera permission persists after updatePermissions without camera param`() = runTest {
        val viewModel = createTestViewModel()

        viewModel.setCameraPermission(true)
        assertTrue(viewModel.hasCameraPermission.value)

        // updatePermissions without camera param should keep existing value
        viewModel.updatePermissions(notificationAccess = true, batteryOptDisabled = false)

        assertTrue(viewModel.hasCameraPermission.value)
    }

    @Test
    fun `connectionState reflects health monitor state`() = runTest {
        val connectionState = MutableStateFlow(HealthMonitor.ConnectionState.DISCONNECTED)
        val viewModel = createTestViewModel(connectionState = connectionState)

        assertEquals(HealthMonitor.ConnectionState.DISCONNECTED, viewModel.connectionState.value)

        connectionState.value = HealthMonitor.ConnectionState.CONNECTING
        assertEquals(HealthMonitor.ConnectionState.CONNECTING, viewModel.connectionState.value)

        connectionState.value = HealthMonitor.ConnectionState.CONNECTED
        assertEquals(HealthMonitor.ConnectionState.CONNECTED, viewModel.connectionState.value)
    }

    @Test
    fun `serverUrl reflects health monitor url`() = runTest {
        val serverUrl = MutableStateFlow<String?>(null)
        val viewModel = createTestViewModel(serverUrl = serverUrl)

        assertEquals(null, viewModel.serverUrl.value)

        serverUrl.value = "https://192.168.1.100:8443"
        assertEquals("https://192.168.1.100:8443", viewModel.serverUrl.value)
    }

    @Test
    fun `isConfigured reflects repository state`() = runTest {
        val isConfigured = MutableStateFlow(false)
        val viewModel = createTestViewModel(isConfigured = isConfigured)

        // Initial value
        assertFalse(viewModel.isConfigured.value)

        // Update and verify
        isConfigured.value = true
        assertTrue(viewModel.isConfigured.value)
    }

    @Test
    fun `serviceEnabled reflects repository state`() = runTest {
        val serviceEnabled = MutableStateFlow(false)
        val viewModel = createTestViewModel(serviceEnabled = serviceEnabled)

        // Initial value
        assertFalse(viewModel.serviceEnabled.value)

        // Update and verify
        serviceEnabled.value = true
        assertTrue(viewModel.serviceEnabled.value)
    }

    // Helper to create ViewModel with fake dependencies
    private fun createTestViewModel(
        isConfigured: MutableStateFlow<Boolean> = MutableStateFlow(false),
        serviceEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false),
        connectionState: MutableStateFlow<HealthMonitor.ConnectionState> =
            MutableStateFlow(HealthMonitor.ConnectionState.DISCONNECTED),
        serverUrl: MutableStateFlow<String?> = MutableStateFlow(null),
    ): TestableMainScreenViewModel {
        return TestableMainScreenViewModel(
            isConfiguredFlow = isConfigured,
            serviceEnabledFlow = serviceEnabled,
            connectionStateFlow = connectionState,
            serverUrlFlow = serverUrl,
        )
    }
}
