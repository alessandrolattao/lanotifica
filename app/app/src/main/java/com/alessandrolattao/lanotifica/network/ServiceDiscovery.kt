package com.alessandrolattao.lanotifica.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Discovers LaNotifica server on the local network using mDNS/DNS-SD. Uses Android's built-in
 * NsdManager API.
 */
class ServiceDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    companion object {
        private const val TAG = "ServiceDiscovery"
        private const val SERVICE_TYPE = "_lanotifica._tcp."
        private const val DISCOVERY_TIMEOUT_MS = 10_000L
    }

    data class DiscoveredServer(val host: String, val port: Int) {
        val url: String
            get() = "https://$host:$port"
    }

    /**
     * Discovers the LaNotifica server on the local network. Returns the server info if found within
     * timeout, null otherwise.
     */
    suspend fun discoverServer(): DiscoveredServer? {
        return withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { discoverServerInternal() }
    }

    private suspend fun discoverServerInternal(): DiscoveredServer? =
        suspendCancellableCoroutine { continuation ->
            var discoveryListener: NsdManager.DiscoveryListener? = null
            var serviceInfoCallback: NsdManager.ServiceInfoCallback? = null
            var resolved = false
            val executor = Executors.newSingleThreadExecutor()

            fun cleanup() {
                try {
                    serviceInfoCallback?.let { nsdManager.unregisterServiceInfoCallback(it) }
                    discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during cleanup: ${e.message}")
                }
                // Don't shutdown executor here - NsdManager may still be delivering callbacks
                // The executor will be garbage collected when no longer referenced
            }

            discoveryListener =
                object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e(TAG, "Discovery start failed: $errorCode")
                        if (!resolved && continuation.isActive) {
                            resolved = true
                            cleanup()
                            continuation.resume(null)
                        }
                    }

                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.w(TAG, "Discovery stop failed: $errorCode")
                    }

                    override fun onDiscoveryStarted(serviceType: String) {
                        Log.d(TAG, "Discovery started for $serviceType")
                    }

                    override fun onDiscoveryStopped(serviceType: String) {
                        Log.d(TAG, "Discovery stopped")
                    }

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                        if (!resolved) {
                            serviceInfoCallback =
                                object : NsdManager.ServiceInfoCallback {
                                    override fun onServiceInfoCallbackRegistrationFailed(
                                        errorCode: Int
                                    ) {
                                        Log.e(
                                            TAG,
                                            "Service info callback registration failed: $errorCode",
                                        )
                                        if (!resolved && continuation.isActive) {
                                            resolved = true
                                            cleanup()
                                            continuation.resume(null)
                                        }
                                    }

                                    override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                                        val hostAddress =
                                            serviceInfo.hostAddresses.firstOrNull()?.hostAddress
                                        Log.d(
                                            TAG,
                                            "Service resolved: $hostAddress:${serviceInfo.port}",
                                        )
                                        if (!resolved && continuation.isActive) {
                                            resolved = true
                                            cleanup()
                                            if (hostAddress != null) {
                                                continuation.resume(
                                                    DiscoveredServer(hostAddress, serviceInfo.port)
                                                )
                                            } else {
                                                continuation.resume(null)
                                            }
                                        }
                                    }

                                    override fun onServiceLost() {
                                        Log.d(TAG, "Service lost during resolution")
                                        if (!resolved && continuation.isActive) {
                                            resolved = true
                                            cleanup()
                                            continuation.resume(null)
                                        }
                                    }

                                    override fun onServiceInfoCallbackUnregistered() {
                                        Log.d(TAG, "Service info callback unregistered")
                                    }
                                }
                            nsdManager.registerServiceInfoCallback(
                                serviceInfo,
                                executor,
                                serviceInfoCallback!!,
                            )
                        }
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                    }
                }

            continuation.invokeOnCancellation { cleanup() }

            Log.d(TAG, "Starting discovery for $SERVICE_TYPE")
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
}
