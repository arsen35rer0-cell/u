package com.dpibypass.app.vpn

import android.util.Log
import com.dpibypass.app.data.BypassConfig

/**
 * Core DPI bypass engine. Implements multiple strategies to evade
 * deep packet inspection targeting YouTube/Google services.
 */
class DpiBypassEngine {

    companion object {
        const val TAG = "DpiBypassEngine"

        // YouTube / Google domains to target
        val YOUTUBE_DOMAINS = listOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "googlevideo.com",
            "ytimg.com",
            "ggpht.com",
            "googleapis.com",
            "gstatic.com"
        )
    }

    private var config = BypassConfig()
    private val parser = PacketParser()

    fun configure(config: BypassConfig) {
        this.config = config
        Log.i(TAG, "Engine configured: fragSize=${config.fragmentSize}, fragDelay=${config.fragmentDelay}")
    }

    /**
     * Process a raw IP packet and return list of packets to send.
     * May return multiple packets if fragmentation is applied.
     */
    fun processPacket(rawPacket: ByteArray, packetParser: PacketParser): List<ByteArray> {
        val parsed = packetParser.parse(rawPacket) ?: return listOf(rawPacket)

        val tcpHeader = parsed.transportHeader as? PacketParser.TcpHeader
        val udpHeader = parsed.transportHeader as? PacketParser.UdpHeader

        // Block QUIC if configured
        if (config.blockQuic && udpHeader != null) {
            if (packetParser.isQuicPacket(parsed)) {
                Log.d(TAG, "Blocking QUIC packet to port ${udpHeader.destPort}")
                return emptyList() // Drop packet
            }
        }

        // Only process TCP HTTPS traffic
        if (tcpHeader == null) return listOf(rawPacket)
        if (tcpHeader.destPort != 443 && tcpHeader.sourcePort != 443) return listOf(rawPacket)

        // Skip non-data packets
        if (parsed.payload.isEmpty()) return listOf(rawPacket)
        if (tcpHeader.isSyn || tcpHeader.isFin || tcpHeader.isRst) return listOf(rawPacket)

        // Check if it's TLS ClientHello (main DPI target)
        val isClientHello = packetParser.isTlsClientHello(parsed.payload)

        return if (isClientHello && config.tcpFragmentation) {
            fragmentClientHello(parsed, tcpHeader, packetParser)
        } else if (config.fakeAck && tcpHeader.isAck && !tcpHeader.isPsh && parsed.payload.isEmpty()) {
            // Optionally inject fake ACKs for noise
            listOf(rawPacket)
        } else {
            listOf(rawPacket)
        }
    }

    /**
     * Fragment TLS ClientHello to split SNI across TCP segments
     */
    private fun fragmentClientHello(
        parsed: PacketParser.ParsedPacket,
        tcpHeader: PacketParser.TcpHeader,
        packetParser: PacketParser
    ): List<ByteArray> {
        val payload = parsed.payload
        val sniPos = packetParser.findSniPosition(payload)

        // Determine split point
        val splitPoint = if (sniPos > 0 && sniPos < payload.size) {
            // Split right before the hostname to split SNI
            (sniPos - 1).coerceIn(1, payload.size - 1)
        } else {
            // Fallback: split at configured fragment size
            config.fragmentSize.coerceIn(1, payload.size - 1)
        }

        val fragment1 = payload.copyOfRange(0, splitPoint)
        val fragment2 = payload.copyOfRange(splitPoint, payload.size)

        val packets = mutableListOf<ByteArray>()

        if (config.packetReordering) {
            // Send second fragment first to confuse DPI
            val packet2 = TcpPacket.buildPacket(
                parsed.ipHeader,
                tcpHeader,
                fragment2,
                sequenceOffset = splitPoint.toLong()
            )
            val packet1 = TcpPacket.buildPacket(
                parsed.ipHeader,
                tcpHeader,
                fragment1,
                sequenceOffset = 0
            )
            packets.add(packet2)
            packets.add(packet1)
            Log.d(TAG, "Reordered fragments: ${fragment2.size}B then ${fragment1.size}B")
        } else {
            val packet1 = TcpPacket.buildPacket(
                parsed.ipHeader,
                tcpHeader,
                fragment1,
                sequenceOffset = 0
            )
            val packet2 = TcpPacket.buildPacket(
                parsed.ipHeader,
                tcpHeader,
                fragment2,
                sequenceOffset = splitPoint.toLong()
            )
            packets.add(packet1)
            packets.add(packet2)
            Log.d(TAG, "Fragmented ClientHello: ${fragment1.size}B + ${fragment2.size}B at SNI pos $sniPos")
        }

        return packets
    }
}
