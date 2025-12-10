package com.alessandrolattao.lanotifica.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
