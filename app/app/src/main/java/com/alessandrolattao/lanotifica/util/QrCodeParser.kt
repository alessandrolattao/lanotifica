package com.alessandrolattao.lanotifica.util

data class ServerConfig(val token: String, val fingerprint: String)

object QrCodeParser {
    /**
     * Parses QR code data in format: token|fingerprint Server URL is discovered via mDNS, not
     * stored in QR.
     */
    fun parse(qrData: String): ServerConfig? {
        val parts = qrData.split("|")
        if (parts.size != 2) return null
        val token = parts[0].trim()
        val fingerprint = parts[1].trim()
        if (token.isBlank() || fingerprint.isBlank()) return null
        return ServerConfig(token, fingerprint)
    }
}
