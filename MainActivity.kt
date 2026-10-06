package com.dpibypass.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dpibypass.app.data.AppPreferences
import com.dpibypass.app.data.BypassConfig
import com.dpibypass.app.ui.screens.MainScreen
import com.dpibypass.app.ui.screens.SettingsScreen
import com.dpibypass.app.ui.theme.DPIBypassTheme
import com.dpibypass.app.ui.theme.DarkBackground
import com.dpibypass.app.vpn.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var vpnService: VpnTunnelService? = null
    private var serviceBound = false
    private lateinit var preferences: AppPreferences

    private val vpnRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? VpnTunnelService.LocalBinder
            vpnService = localBinder?.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vpnService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        setContent {
            DPIBypassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        val navController = rememberNavController()

        val vpnState by VpnTunnelService.state.collectAsState()
        val bytesTransferred by VpnTunnelService.bytesTransferred.collectAsState()
        val packetsProcessed by VpnTunnelService.packetsProcessed.collectAsState()

        var config by remember { mutableStateOf(BypassConfig()) }

        LaunchedEffect(Unit) {
            preferences.getConfig().collectLatest { config = it }
        }

        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    vpnState = vpnState,
                    bytesTransferred = bytesTransferred,
                    packetsProcessed = packetsProcessed,
                    onStartVpn = { requestVpnPermission() },
                    onStopVpn = { stopVpn() },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    config = config,
                    onConfigChange = { newConfig ->
                        config = newConfig
                        lifecycleScope.launch {
                            preferences.saveConfig(newConfig)
                            vpnService?.let {
                                // Hot-reload config
                                DpiBypassEngine().configure(newConfig)
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnRequestLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, VpnTunnelService::class.java).apply {
            action = ACTION_START
        }
        startForegroundService(intent)
        bindService(
            Intent(this, VpnTunnelService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun stopVpn() {
        vpnService?.stopTunnel()
        val intent = Intent(this, VpnTunnelService::class.java).apply {
            action = ACTION_STOP
        }
        startService(intent)
    }

    override fun onStart() {
        super.onStart()
        if (!serviceBound) {
            bindService(
                Intent(this, VpnTunnelService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
                serviceBound = false
            } catch (e: Exception) {
                // Already unbound
            }
        }
    }
}
