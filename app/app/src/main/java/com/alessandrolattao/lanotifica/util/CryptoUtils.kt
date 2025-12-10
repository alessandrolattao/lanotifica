package com.alessandrolattao.lanotifica.util

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object CryptoUtils {
    fun calculateFingerprint(cert: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cert.encoded)
        return digest.joinToString("") { "%02X".format(it) }
    }

    fun calculateFingerprint(certBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    fun fingerprintsMatch(a: String, b: String): Boolean {
        return a.equals(b, ignoreCase = true)
    }

    /**
     * Creates an X509TrustManager that validates server certificates against an expected fingerprint.
     * Used for certificate pinning.
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    fun createPinningTrustManager(expectedFingerprint: String): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Client certificates not used
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain.isNullOrEmpty()) {
                    throw IllegalStateException("No certificate provided by server")
                }
                val serverFingerprint = calculateFingerprint(chain[0])
                if (!fingerprintsMatch(serverFingerprint, expectedFingerprint)) {
                    throw IllegalStateException(
                        "Certificate fingerprint mismatch! Expected: $expectedFingerprint, Got: $serverFingerprint"
                    )
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Creates an OkHttpClient configured with certificate pinning.
     * Verifies server certificate fingerprint instead of relying on CA chain.
     */
    fun createPinnedOkHttpClient(
        fingerprint: String,
        connectTimeoutMs: Long = 10_000,
        readTimeoutMs: Long = 10_000,
        writeTimeoutMs: Long = 10_000
    ): OkHttpClient {
        val trustManager = createPinningTrustManager(fingerprint)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), null)
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // We verify via fingerprint instead
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }
}
