package com.dpibypass.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dpibypass.app.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = AppPreferences(context)
                if (prefs.isAutoStartEnabled().first()) {
                    val vpnIntent = VpnTunnelService().let {
                        Intent(context, VpnTunnelService::class.java).apply {
                            action = ACTION_START
                        }
                    }
                    context.startForegroundService(vpnIntent)
                }
            }
        }
    }
}
