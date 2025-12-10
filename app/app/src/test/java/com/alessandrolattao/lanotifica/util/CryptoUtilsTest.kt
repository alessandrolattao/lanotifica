package com.alessandrolattao.lanotifica.util

import io.mockk.every
import io.mockk.mockk
import java.security.cert.X509Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun `calculateFingerprint produces uppercase hex`() {
        val testData = "test".toByteArray()
        val fingerprint = CryptoUtils.calculateFingerprint(testData)

        // SHA-256 of "test" is known value
        assertEquals(
            "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08",
            fingerprint,
        )
    }

    @Test
    fun `calculateFingerprint is consistent`() {
        val testData = "hello world".toByteArray()
        val first = CryptoUtils.calculateFingerprint(testData)
        val second = CryptoUtils.calculateFingerprint(testData)

        assertEquals(first, second)
    }

    @Test
    fun `calculateFingerprint has correct length`() {
        val testData = "any data".toByteArray()
        val fingerprint = CryptoUtils.calculateFingerprint(testData)

        // SHA-256 produces 32 bytes = 64 hex characters
        assertEquals(64, fingerprint.length)
    }

    @Test
    fun `fingerprintsMatch is case insensitive`() {
        assertTrue(CryptoUtils.fingerprintsMatch("ABC123", "abc123"))
        assertTrue(CryptoUtils.fingerprintsMatch("abc123", "ABC123"))
        assertTrue(CryptoUtils.fingerprintsMatch("AbC123", "aBc123"))
    }

    @Test
    fun `fingerprintsMatch returns true for identical fingerprints`() {
        val fp = "A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4E5F6A1B2"
        assertTrue(CryptoUtils.fingerprintsMatch(fp, fp))
    }

    @Test
    fun `fingerprintsMatch returns false for different fingerprints`() {
        assertFalse(CryptoUtils.fingerprintsMatch("ABC123", "ABC124"))
        assertFalse(CryptoUtils.fingerprintsMatch("ABC123", "DEF456"))
    }

    @Test
    fun `fingerprintsMatch handles empty strings`() {
        assertTrue(CryptoUtils.fingerprintsMatch("", ""))
        assertFalse(CryptoUtils.fingerprintsMatch("", "ABC"))
        assertFalse(CryptoUtils.fingerprintsMatch("ABC", ""))
    }

    // TrustManager tests

    @Test
    fun `createPinningTrustManager returns non-null`() {
        val trustManager = CryptoUtils.createPinningTrustManager("test-fingerprint")
        assertNotNull(trustManager)
    }

    @Test
    fun `createPinningTrustManager returns empty accepted issuers`() {
        val trustManager = CryptoUtils.createPinningTrustManager("test-fingerprint")
        assertTrue(trustManager.acceptedIssuers.isEmpty())
    }

    @Test
    fun `createPinningTrustManager checkServerTrusted throws for null chain`() {
        val trustManager = CryptoUtils.createPinningTrustManager("test-fingerprint")

        try {
            trustManager.checkServerTrusted(null, "RSA")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("No certificate") == true)
        }
    }

    @Test
    fun `createPinningTrustManager checkServerTrusted throws for empty chain`() {
        val trustManager = CryptoUtils.createPinningTrustManager("test-fingerprint")

        try {
            trustManager.checkServerTrusted(emptyArray(), "RSA")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("No certificate") == true)
        }
    }

    @Test
    fun `createPinningTrustManager checkServerTrusted accepts matching fingerprint`() {
        // Create a mock certificate with known bytes
        val certBytes = "test-certificate-data".toByteArray()
        val expectedFingerprint = CryptoUtils.calculateFingerprint(certBytes)

        val mockCert = mockk<X509Certificate>()
        every { mockCert.encoded } returns certBytes

        val trustManager = CryptoUtils.createPinningTrustManager(expectedFingerprint)

        // Should not throw
        trustManager.checkServerTrusted(arrayOf(mockCert), "RSA")
    }

    @Test
    fun `createPinningTrustManager checkServerTrusted rejects wrong fingerprint`() {
        // Create a mock certificate
        val certBytes = "test-certificate-data".toByteArray()

        val mockCert = mockk<X509Certificate>()
        every { mockCert.encoded } returns certBytes

        val trustManager = CryptoUtils.createPinningTrustManager("WRONG_FINGERPRINT")

        try {
            trustManager.checkServerTrusted(arrayOf(mockCert), "RSA")
            fail("Expected IllegalStateException for fingerprint mismatch")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("mismatch") == true)
        }
    }

    @Test
    fun `createPinningTrustManager fingerprint comparison is case insensitive`() {
        val certBytes = "test-certificate-data".toByteArray()
        val expectedFingerprint = CryptoUtils.calculateFingerprint(certBytes)

        val mockCert = mockk<X509Certificate>()
        every { mockCert.encoded } returns certBytes

        // Use lowercase fingerprint
        val trustManager = CryptoUtils.createPinningTrustManager(expectedFingerprint.lowercase())

        // Should not throw (case insensitive comparison)
        trustManager.checkServerTrusted(arrayOf(mockCert), "RSA")
    }

    // OkHttpClient tests

    @Test
    fun `createPinnedOkHttpClient returns non-null client`() {
        val client = CryptoUtils.createPinnedOkHttpClient("test-fingerprint")
        assertNotNull(client)
    }

    @Test
    fun `createPinnedOkHttpClient respects timeout parameters`() {
        val client = CryptoUtils.createPinnedOkHttpClient(
            fingerprint = "test-fingerprint",
            connectTimeoutMs = 5000,
            readTimeoutMs = 3000,
            writeTimeoutMs = 2000
        )

        assertEquals(5000, client.connectTimeoutMillis)
        assertEquals(3000, client.readTimeoutMillis)
        assertEquals(2000, client.writeTimeoutMillis)
    }

    @Test
    fun `createPinnedOkHttpClient uses default timeouts`() {
        val client = CryptoUtils.createPinnedOkHttpClient("test-fingerprint")

        assertEquals(10000, client.connectTimeoutMillis)
        assertEquals(10000, client.readTimeoutMillis)
        assertEquals(10000, client.writeTimeoutMillis)
    }
}
