package com.alessandrolattao.lanotifica.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.alessandrolattao.lanotifica.data.SettingsRepository
import com.alessandrolattao.lanotifica.di.AppModule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationForwarderServiceTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val processedPackages = mutableListOf<String>()

    @Before
    fun setup() {
        mockkObject(AppModule)
        every { AppModule.settingsRepository } returns settingsRepository
        // Every forwarded notification first records its app, then stops at serviceEnabled
        coEvery { settingsRepository.addKnownApp(any()) } answers
            {
                processedPackages += firstArg<String>()
            }
        every { settingsRepository.serviceEnabled } returns flowOf(false)
        // Run the service coroutines inline so the assertions see their effects
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns Dispatchers.Unconfined
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `regular notification is processed`() {
        val service =
            Robolectric.buildService(NotificationForwarderService::class.java).create().get()

        service.onNotificationPosted(statusBarNotification(isGroupSummary = false))

        assertEquals(listOf(PACKAGE), processedPackages)
    }

    @Test
    fun `group summary is ignored`() {
        val service =
            Robolectric.buildService(NotificationForwarderService::class.java).create().get()

        service.onNotificationPosted(statusBarNotification(isGroupSummary = true))

        assertEquals(emptyList<String>(), processedPackages)
    }

    private fun statusBarNotification(isGroupSummary: Boolean): StatusBarNotification {
        val notification =
            Notification.Builder(RuntimeEnvironment.getApplication(), "messages")
                .setContentTitle("Francesca Castellano")
                .setContentText("Prova")
                .setGroup("chat")
                .setGroupSummary(isGroupSummary)
                .build()
        return mockk {
            every { packageName } returns PACKAGE
            every { key } returns "0|$PACKAGE|${if (isGroupSummary) 2 else 1}|null|10100"
            every { isOngoing } returns false
            every { this@mockk.notification } returns notification
        }
    }

    private companion object {
        const val PACKAGE = "com.whatsapp"
    }
}
