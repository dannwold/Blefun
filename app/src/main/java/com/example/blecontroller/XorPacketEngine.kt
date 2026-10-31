package com.example.blecontroller

object XorPacketEngine {

    /**
     * Computes single-byte XOR across all bytes in the array.
     */
    fun calculateXorChecksum(bytes: ByteArray): Byte {
        var checksum: Byte = 0
        for (b in bytes) {
            checksum = (checksum.toInt() xor b.toInt()).toByte()
        }
        return checksum
    }

    /**
     * Allocates a 20-byte array.
     * Sets pkt[0] = header, pkt[1] = cmd.
     * Copies up to 17 bytes of payload into pkt[2..18].
     * Computes XOR across pkt[0..18] and writes the result into pkt[19].
     */
    fun build20BytePaddedXorPacket(header: Int, cmd: Int, payload: ByteArray): ByteArray {
        val pkt = ByteArray(20) { 0 }
        pkt[0] = header.toByte()
        pkt[1] = cmd.toByte()

        val copyLength = minOf(payload.size, 17)
        for (i in 0 until copyLength) {
            pkt[2 + i] = payload[i]
        }

        var checksum: Byte = 0
        for (i in 0..18) {
            checksum = (checksum.toInt() xor pkt[i].toInt()).toByte()
        }
        pkt[19] = checksum

        return pkt
    }

    /**
     * Returns rawBytes with calculateXorChecksum(rawBytes) appended at the end if appendXor == true.
     */
    fun buildVariableXorPacket(rawBytes: ByteArray, appendXor: Boolean): ByteArray {
        if (!appendXor) return rawBytes

        val checksum = calculateXorChecksum(rawBytes)
        val result = ByteArray(rawBytes.size + 1)
        System.arraycopy(rawBytes, 0, result, 0, rawBytes.size)
        result[result.size - 1] = checksum
        return result
    }

    /**
     * Applies a repeating-key XOR cipher (challenge[i] ^ key[i % key.size]) for dynamic BLE authentication handshakes.
     */
    fun xorChallengeResponse(challenge: ByteArray, key: ByteArray): ByteArray {
        if (key.isEmpty()) return challenge
        val result = ByteArray(challenge.size)
        for (i in challenge.indices) {
            result[i] = (challenge[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }
}
