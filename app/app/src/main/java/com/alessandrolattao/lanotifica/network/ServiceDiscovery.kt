package com.alessandrolattao.lanotifica.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
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
            var resolved = false

            val resolveListener =
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                        if (!resolved && continuation.isActive) {
                            resolved = true
                            continuation.resume(null)
                        }
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(
                            TAG,
                            "Service resolved: ${serviceInfo.host?.hostAddress}:${serviceInfo.port}",
                        )
                        if (!resolved && continuation.isActive) {
                            resolved = true
                            val host = serviceInfo.host?.hostAddress
                            if (host != null) {
                                continuation.resume(DiscoveredServer(host, serviceInfo.port))
                            } else {
                                continuation.resume(null)
                            }
                        }
                        // Stop discovery after finding the server
                        try {
                            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error stopping discovery: ${e.message}")
                        }
                    }
                }

            discoveryListener =
                object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.e(TAG, "Discovery start failed: $errorCode")
                        if (!resolved && continuation.isActive) {
                            resolved = true
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
                            nsdManager.resolveService(serviceInfo, resolveListener)
                        }
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                    }
                }

            continuation.invokeOnCancellation {
                try {
                    discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping discovery on cancellation: ${e.message}")
                }
            }

            Log.d(TAG, "Starting discovery for $SERVICE_TYPE")
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
}
