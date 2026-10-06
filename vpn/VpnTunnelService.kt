package com.dpibypass.app.vpn

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dpibypass.app.R
import com.dpibypass.app.data.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class VpnTunnelService : VpnService() {

    companion object {
        const val TAG = "VpnTunnelService"
        const val NOTIFICATION_CHANNEL_ID = "vpn_channel"
        const val NOTIFICATION_ID = 1001

        private val _state = MutableStateFlow(VpnState.DISCONNECTED)
        val state: StateFlow<VpnState> = _state

        private val _bytesTransferred = MutableStateFlow(0L)
        val bytesTransferred: StateFlow<Long> = _bytesTransferred

        private val _packetsProcessed = MutableStateFlow(0L)
        val packetsProcessed: StateFlow<Long> = _packetsProcessed
    }

    private val binder = LocalBinder()
    private var tunnelInterface: ParcelFileDescriptor? = null
    private var processingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var dpiEngine: DpiBypassEngine
    private lateinit var preferences: AppPreferences

    inner class LocalBinder : Binder() {
        fun getService(): VpnTunnelService = this@VpnTunnelService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        preferences = AppPreferences(this)
        dpiEngine = DpiBypassEngine()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTunnel()
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (_state.value == VpnState.CONNECTED || _state.value == VpnState.CONNECTING) {
            return
        }

        _state.value = VpnState.CONNECTING
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                val config = preferences.getConfig()
                dpiEngine.configure(config)

                val builder = Builder().apply {
                    setSession("DPI Bypass")
                    addAddress("10.0.0.2", 32)
                    addDnsServer("8.8.8.8")
                    addDnsServer("1.1.1.1")
                    addRoute("0.0.0.0", 0)
                    setMtu(1500)

                    // Allow YouTube apps through, block others from bypass
                    try {
                        // Allow all apps by default - VPN catches everything
                        addDisallowedApplication(packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "App package not found", e)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setMetered(false)
                    }
                }

                tunnelInterface = builder.establish()
                if (tunnelInterface == null) {
                    _state.value = VpnState.ERROR
                    stopSelf()
                    return@launch
                }

                _state.value = VpnState.CONNECTED
                startPacketProcessing()

                Log.i(TAG, "VPN tunnel established")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start tunnel", e)
                _state.value = VpnState.ERROR
                stopSelf()
            }
        }
    }

    private fun startPacketProcessing() {
        val fd = tunnelInterface?.fileDescriptor ?: return

        processingJob = serviceScope.launch {
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            val buffer = ByteArray(32768)
            val packetParser = PacketParser()

            while (isActive) {
                try {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) {
                        delay(10)
                        continue
                    }

                    val packetData = buffer.copyOf(bytesRead)
                    _bytesTransferred.value += bytesRead
                    _packetsProcessed.value++

                    // Process packet through DPI bypass engine
                    val processedPackets = dpiEngine.processPacket(packetData, packetParser)

                    // Send all resulting packets
                    for (packet in processedPackets) {
                        output.write(packet)
                        output.flush()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Packet processing error", e)
                    delay(50)
                }
            }
        }
    }

    fun stopTunnel() {
        Log.i(TAG, "Stopping VPN tunnel")
        processingJob?.cancel()
        try {
            tunnelInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing tunnel", e)
        }
        tunnelInterface = null
        _state.value = VpnState.DISCONNECTED
        _bytesTransferred.value = 0
        _packetsProcessed.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN service notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

const val ACTION_START = "com.dpibypass.app.ACTION_START"
const val ACTION_STOP = "com.dpibypass.app.ACTION_STOP"
