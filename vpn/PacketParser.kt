package com.dpibypass.app.vpn

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for raw IP packets from TUN interface.
 * Handles IPv4 and TCP/UDP dissection.
 */
class PacketParser {

    data class IpHeader(
        val version: Int,
        val headerLength: Int,
        val totalLength: Int,
        val protocol: Int,
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val rawHeader: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IpHeader) return false
            return version == other.version && totalLength == other.totalLength
        }
        override fun hashCode(): Int = totalLength
    }

    data class TcpHeader(
        val sourcePort: Int,
        val destPort: Int,
        val sequenceNumber: Long,
        val ackNumber: Long,
        val dataOffset: Int,
        val flags: Int,
        val windowSize: Int,
        val checksum: Int,
        val rawHeader: ByteArray
    ) {
        val isSyn: Boolean get() = (flags and 0x02) != 0
        val isAck: Boolean get() = (flags and 0x10) != 0
        val isPsh: Boolean get() = (flags and 0x08) != 0
        val isFin: Boolean get() = (flags and 0x01) != 0
        val isRst: Boolean get() = (flags and 0x04) != 0
    }

    data class UdpHeader(
        val sourcePort: Int,
        val destPort: Int,
        val length: Int,
        val checksum: Int
    )

    data class ParsedPacket(
        val ipHeader: IpHeader,
        val transportHeader: Any?, // TcpHeader or UdpHeader
        val payload: ByteArray,
        val rawPacket: ByteArray
    )

    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17
        const val HTTPS_PORT = 443
        const val QUIC_PORT = 443

        // TLS record types
        const val TLS_HANDSHAKE = 22
        const val TLS_CLIENT_HELLO = 1
    }

    fun parse(packet: ByteArray): ParsedPacket? {
        if (packet.size < 20) return null

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)

        // Parse IP header
        val versionIhl = buffer.get().toInt() and 0xFF
        val version = (versionIhl shr 4) and 0x0F
        if (version != 4) return null // Only IPv4 supported

        val ihl = (versionIhl and 0x0F) * 4
        if (packet.size < ihl) return null

        buffer.position(2)
        val totalLength = buffer.short.toInt() and 0xFFFF
        buffer.position(9)
        val protocol = buffer.get().toInt() and 0xFF

        val sourceIp = ByteArray(4)
        val destIp = ByteArray(4)
        buffer.position(12)
        buffer.get(sourceIp)
        buffer.get(destIp)

        val rawIpHeader = packet.copyOf(ihl)
        val ipHeader = IpHeader(
            version = version,
            headerLength = ihl,
            totalLength = totalLength,
            protocol = protocol,
            sourceIp = sourceIp,
            destIp = destIp,
            rawHeader = rawIpHeader
        )

        // Parse transport header
        val transportHeader: Any?
        val payloadStart: Int

        when (protocol) {
            PROTOCOL_TCP -> {
                if (packet.size < ihl + 20) return null
                buffer.position(ihl)
                val srcPort = buffer.short.toInt() and 0xFFFF
                val dstPort = buffer.short.toInt() and 0xFFFF
                val seqNum = buffer.int.toLong() and 0xFFFFFFFFL
                val ackNum = buffer.int.toLong() and 0xFFFFFFFFL
                val dataOffsetFlags = buffer.short.toInt() and 0xFFFF
                val dataOffset = ((dataOffsetFlags shr 12) and 0x0F) * 4
                val flags = dataOffsetFlags and 0x3F
                val window = buffer.short.toInt() and 0xFFFF
                val checksum = buffer.short.toInt() and 0xFFFF

                val rawTcpHeader = packet.copyOfRange(ihl, ihl + dataOffset)
                transportHeader = TcpHeader(
                    sourcePort = srcPort,
                    destPort = dstPort,
                    sequenceNumber = seqNum,
                    ackNumber = ackNum,
                    dataOffset = dataOffset,
                    flags = flags,
                    windowSize = window,
                    checksum = checksum,
                    rawHeader = rawTcpHeader
                )
                payloadStart = ihl + dataOffset
            }
            PROTOCOL_UDP -> {
                if (packet.size < ihl + 8) return null
                buffer.position(ihl)
                val srcPort = buffer.short.toInt() and 0xFFFF
                val dstPort = buffer.short.toInt() and 0xFFFF
                val length = buffer.short.toInt() and 0xFFFF
                val checksum = buffer.short.toInt() and 0xFFFF
                transportHeader = UdpHeader(srcPort, dstPort, length, checksum)
                payloadStart = ihl + 8
            }
            else -> return null
        }

        val payload = if (payloadStart < packet.size) {
            packet.copyOfRange(payloadStart, packet.size)
        } else ByteArray(0)

        return ParsedPacket(ipHeader, transportHeader, payload, packet)
    }

    /**
     * Check if payload contains TLS ClientHello with SNI
     */
    fun isTlsClientHello(payload: ByteArray): Boolean {
        if (payload.size < 6) return false
        // TLS record: type (1 byte) + version (2 bytes) + length (2 bytes)
        if (payload[0].toInt() != TLS_HANDSHAKE) return false
        // Handshake type
        if (payload.size < 6 + 1) return false
        if (payload[5].toInt() != TLS_CLIENT_HELLO) return false
        return true
    }

    /**
     * Find SNI position in TLS ClientHello payload
     */
    fun findSniPosition(payload: ByteArray): Int {
        if (!isTlsClientHello(payload)) return -1

        try {
            var offset = 5 // Skip TLS record header
            offset += 1 // Handshake type
            offset += 3 // Handshake length
            offset += 2 // Client version
            offset += 32 // Random
            offset += 1 + (payload[offset].toInt() and 0xFF) // Session ID
            offset += 2 + ((payload[offset].toInt() and 0xFF shl 8) or (payload[offset + 1].toInt() and 0xFF)) // Cipher suites
            offset += 1 + (payload[offset].toInt() and 0xFF) // Compression methods

            // Extensions length
            if (offset + 2 > payload.size) return -1
            val extLen = ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)
            offset += 2
            val extEnd = offset + extLen

            while (offset + 4 <= extEnd && offset + 4 <= payload.size) {
                val extType = ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)
                val extDataLen = ((payload[offset + 2].toInt() and 0xFF) shl 8) or (payload[offset + 3].toInt() and 0xFF)
                offset += 4

                if (extType == 0x0000) { // SNI extension
                    // SNI list length (2) + type (1) + name length (2)
                    if (offset + 5 > payload.size) return -1
                    return offset + 3 + 2 + 1 // Position of actual hostname bytes
                }

                offset += extDataLen
            }
        } catch (e: Exception) {
            return -1
        }
        return -1
    }

    fun isQuicPacket(parsed: ParsedPacket): Boolean {
        val udp = parsed.transportHeader as? UdpHeader ?: return false
        if (udp.destPort != 443 && udp.sourcePort != 443) return false
        // QUIC uses UDP on port 443, first byte is connection ID length indicator
        if (parsed.payload.size < 5) return false
        // Heuristic: QUIC long header starts with 0x80 bit set or short header
        val firstByte = parsed.payload[0].toInt() and 0xFF
        return (firstByte and 0x80) != 0 || parsed.payload.size > 100
    }
}
