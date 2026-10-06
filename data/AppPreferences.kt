package com.dpibypass.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class BypassConfig(
    val tcpFragmentation: Boolean = true,
    val packetReordering: Boolean = true,
    val sniSpoofing: Boolean = true,
    val blockQuic: Boolean = true,
    val fakeAck: Boolean = false,
    val fragmentSize: Int = 6,
    val fragmentDelay: Int = 0
)

private val Context.dataStore by preferencesDataStore(name = "dpi_settings")

class AppPreferences(private val context: Context) {

    private object Keys {
        val TCP_FRAGMENTATION = booleanPreferencesKey("tcp_fragmentation")
        val PACKET_REORDERING = booleanPreferencesKey("packet_reordering")
        val SNI_SPOOFING = booleanPreferencesKey("sni_spoofing")
        val BLOCK_QUIC = booleanPreferencesKey("block_quic")
        val FAKE_ACK = booleanPreferencesKey("fake_ack")
        val FRAGMENT_SIZE = intPreferencesKey("fragment_size")
        val FRAGMENT_DELAY = intPreferencesKey("fragment_delay")
        val AUTO_START = booleanPreferencesKey("auto_start")
    }

    fun getConfig(): Flow<BypassConfig> = context.dataStore.data.map { prefs ->
        BypassConfig(
            tcpFragmentation = prefs[Keys.TCP_FRAGMENTATION] ?: true,
            packetReordering = prefs[Keys.PACKET_REORDERING] ?: true,
            sniSpoofing = prefs[Keys.SNI_SPOOFING] ?: true,
            blockQuic = prefs[Keys.BLOCK_QUIC] ?: true,
            fakeAck = prefs[Keys.FAKE_ACK] ?: false,
            fragmentSize = prefs[Keys.FRAGMENT_SIZE] ?: 6,
            fragmentDelay = prefs[Keys.FRAGMENT_DELAY] ?: 0
        )
    }

    suspend fun getConfigSync(): BypassConfig = getConfig().first()

    suspend fun saveConfig(config: BypassConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TCP_FRAGMENTATION] = config.tcpFragmentation
            prefs[Keys.PACKET_REORDERING] = config.packetReordering
            prefs[Keys.SNI_SPOOFING] = config.sniSpoofing
            prefs[Keys.BLOCK_QUIC] = config.blockQuic
            prefs[Keys.FAKE_ACK] = config.fakeAck
            prefs[Keys.FRAGMENT_SIZE] = config.fragmentSize
            prefs[Keys.FRAGMENT_DELAY] = config.fragmentDelay
        }
    }

    fun isAutoStartEnabled(): Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_START] ?: false
    }
}
