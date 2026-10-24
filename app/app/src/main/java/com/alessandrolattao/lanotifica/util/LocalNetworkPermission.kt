package com.alessandrolattao.lanotifica.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat

/**
 * Android 17 blocks local network traffic (mDNS discovery and the server connection) for apps
 * targeting API 37 until ACCESS_LOCAL_NETWORK is granted. Without it, every discovery also makes
 * Android show a device picker, even while the app is in the background.
 */
object LocalNetworkPermission {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CINNAMON_BUN)
    fun isMissing(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) !=
                PackageManager.PERMISSION_GRANTED
}
