package com.dpibypass.app.vpn

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builder for reconstructing TCP packets with modifications
 */
object TcpPacket {

    /**
     * Rebuild IP+TCP packet with new payload, updating lengths and checksums
     */
    fun buildPacket(
        ipHeader: PacketParser.IpHeader,
        tcpHeader: PacketParser.TcpHeader,
        newPayload: ByteArray,
        sequenceOffset: Long = 0
    ): ByteArray {
        val baos = ByteArrayOutputStream()

        // Build TCP header with updated seq and checksum
        val tcpHeaderBytes = buildTcpHeader(tcpHeader, ipHeader, newPayload, sequenceOffset)

        val totalLength = ipHeader.headerLength + tcpHeaderBytes.size + newPayload.size

        // Build IP header with updated length and checksum
        val ipHeaderBytes = buildIpHeader(ipHeader, totalLength)

        baos.write(ipHeaderBytes)
        baos.write(tcpHeaderBytes)
        baos.write(newPayload)

        return baos.toByteArray()
    }

    private fun buildIpHeader(original: PacketParser.IpHeader, totalLength: Int): ByteArray {
        val buf = ByteBuffer.allocate(original.headerLength).order(ByteOrder.BIG_ENDIAN)

        // Copy original header
        buf.put(original.rawHeader)
        buf.clear()

        // Update total length (bytes 2-3)
        buf.position(2)
        buf.putShort(totalLength.toShort())

        // Zero checksum (bytes 10-11) for recalculation
        buf.position(10)
        buf.putShort(0)

        // Calculate IP checksum
        val bytes = buf.array()
        val checksum = calculateChecksum(bytes, 0, original.headerLength)
        buf.position(10)
        buf.putShort(checksum.toShort())

        return bytes
    }

    private fun buildTcpHeader(
        tcpHeader: PacketParser.TcpHeader,
        ipHeader: PacketParser.IpHeader,
        payload: ByteArray,
        sequenceOffset: Long
    ): ByteArray {
        val headerSize = tcpHeader.dataOffset
        val buf = ByteBuffer.allocate(headerSize).order(ByteOrder.BIG_ENDIAN)

        // Copy original TCP header
        val srcLen = minOf(tcpHeader.rawHeader.size, headerSize)
        buf.put(tcpHeader.rawHeader, 0, srcLen)
        if (srcLen < headerSize) {
            buf.put(ByteArray(headerSize - srcLen))
        }
        buf.clear()

        // Update sequence number if offset provided
        if (sequenceOffset != 0L) {
            val newSeq = (tcpHeader.sequenceNumber + sequenceOffset) and 0xFFFFFFFFL
            buf.position(4)
            buf.putInt(newSeq.toInt())
        }

        // Zero TCP checksum
        buf.position(16)
        buf.putShort(0)

        // Calculate TCP checksum with pseudo-header
        val bytes = buf.array()
        val pseudoHeader = buildPseudoHeader(
            ipHeader.sourceIp,
            ipHeader.destIp,
            PacketParser.PROTOCOL_TCP,
            headerSize + payload.size
        )

        val combined = ByteArray(pseudoHeader.size + bytes.size + payload.size)
        System.arraycopy(pseudoHeader, 0, combined, 0, pseudoHeader.size)
        System.arraycopy(bytes, 0, combined, pseudoHeader.size, bytes.size)
        System.arraycopy(payload, 0, combined, pseudoHeader.size + bytes.size, payload.size)

        val checksum = calculateChecksum(combined, 0, combined.size)
        buf.position(16)
        buf.putShort(checksum.toShort())

        return bytes
    }

    private fun buildPseudoHeader(
        srcIp: ByteArray,
        dstIp: ByteArray,
        protocol: Int,
        tcpLength: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        buf.put(srcIp)
        buf.put(dstIp)
        buf.put(0)
        buf.put(protocol.toByte())
        buf.putShort(tcpLength.toShort())
        return buf.array()
    }

    /**
     * Standard Internet checksum (RFC 1071)
     */
    fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length

        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }

        // Handle odd byte
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }

        // Fold 32-bit sum to 16 bits
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF)
    }

    /**
     * Create a TCP RST packet to reset connection
     */
    fun buildResetPacket(
        ipHeader: PacketParser.IpHeader,
        tcpHeader: PacketParser.TcpHeader
    ): ByteArray {
        // Swap src/dst and set RST flag
        val newIpHeader = ipHeader.copy(
            sourceIp = ipHeader.destIp,
            destIp = ipHeader.sourceIp
        )
        val newFlags = tcpHeader.flags or 0x04 // RST flag
        val newTcpHeader = tcpHeader.copy(
            sourcePort = tcpHeader.destPort,
            destPort = tcpHeader.sourcePort,
            sequenceNumber = tcpHeader.ackNumber,
            flags = newFlags,
            rawHeader = tcpHeader.rawHeader.clone()
        )
        // Update flags in raw header
        if (newTcpHeader.rawHeader.size >= 14) {
            val dataOffsetFlags = ((newTcpHeader.dataOffset / 4) shl 12) or newFlags
            newTcpHeader.rawHeader[12] = (dataOffsetFlags shr 8).toByte()
            newTcpHeader.rawHeader[13] = (dataOffsetFlags and 0xFF).toByte()
        }
        return buildPacket(newIpHeader, newTcpHeader, ByteArray(0))
    }
}
