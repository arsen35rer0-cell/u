package com.dpibypass.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.InetAddress

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun bytesToIp(bytes: ByteArray): String {
        return try {
            InetAddress.getByAddress(bytes).hostAddress ?: "unknown"
        } catch (e: Exception) {
            bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
        }
    }
}
