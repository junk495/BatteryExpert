package com.batteryexpert.data.ble

import com.batteryexpert.data.db.ChargeProfileEntity
import java.util.Locale

data class SlotStatus(
    val slot: Int,
    val voltageV: Float,
    val currentA: Float,
    val temperatureC: Float,
    val capacityMah: Int,
    val elapsedSeconds: Long,
    val internalResistanceMOhm: Int,
    val status: String,
    val mode: String,
    val error: String,
    val chemistry: String
)

class ProtocolCodec {

    fun buildPacket(command: Int, data: ByteArray = byteArrayOf()): ByteArray {
        val cmdByte = command and 0xFF
        val length = 1 + data.size + 1 // command + data + checksum
        val sum = cmdByte + data.sumOf { it.toInt() and 0xFF }
        val checksum = (sum and 0xFF).toByte()

        val packet = ByteArray(2 + data.size + 2)
        packet[0] = 0x0F.toByte()
        packet[1] = length.toByte()
        packet[2] = cmdByte.toByte()
        if (data.isNotEmpty()) {
            System.arraycopy(data, 0, packet, 3, data.size)
        }
        packet[packet.lastIndex] = checksum
        return packet
    }

    fun checksumValid(packet: ByteArray): Boolean {
        if (packet.size < 4) return false
        if (packet[0] != 0x0F.toByte()) return false
        val len = packet[1].toInt() and 0xFF
        if (packet.size < len + 2) return false
        val checksumIndex = 1 + len
        val sum = packet.sliceArray(2 until checksumIndex).sumOf { it.toInt() and 0xFF }
        val expectedChecksum = (sum and 0xFF).toByte()
        return packet[checksumIndex] == expectedChecksum
    }

    fun buildStatusRequest(slotBitmask: Int): ByteArray {
        return buildPacket(0x91, byteArrayOf(slotBitmask.toByte()))
    }

    fun buildStartStop(action: Int): ByteArray {
        return buildPacket(0x93, byteArrayOf(action.toByte()))
    }

    fun buildChargeConfig(
        profile: ChargeProfileEntity,
        chemistryCode: Int,
        slotBitmask: Int,
        capacityCutoffMah: Int = 3000
    ): ByteArray {
        val packet = ByteArray(44)
        packet[0] = 0x0F.toByte()
        packet[1] = 0x2A.toByte()
        packet[2] = 0x94.toByte()
        packet[3] = slotBitmask.toByte()

        val modeCode = when (profile.mode.lowercase(Locale.GERMANY)) {
            "charge", "normal charge", "laden" -> 0
            "storage", "lagerung" -> 1
            "discharge", "entladen" -> 2
            "cycle", "zyklus" -> 3
            "refresh", "auffrischen" -> 4
            "break_in", "break-in", "formieren" -> 5
            else -> 0
        }
        packet[4] = modeCode.toByte()

        fun putU16(offset: Int, value: Int) {
            packet[offset] = ((value shr 8) and 0xFF).toByte()
            packet[offset + 1] = (value and 0xFF).toByte()
        }

        putU16(5, profile.chargeCurrentMa)
        putU16(7, profile.dischargeCurrentMa)
        putU16(9, capacityCutoffMah)
        putU16(11, profile.targetVoltageMv)
        putU16(13, profile.cutoffVoltageMv)
        putU16(15, profile.terminationCurrentMa)
        putU16(17, 100)
        putU16(19, profile.restChargeMin)
        putU16(21, profile.restDischargeMin)

        packet[23] = profile.cycleCount.coerceIn(1, 99).toByte()
        packet[24] = profile.cycleDirection.coerceIn(0, 3).toByte()
        packet[25] = profile.deltaPeakMv.coerceIn(0, 255).toByte()
        packet[26] = (profile.trickleChargeMa / 10).coerceIn(0, 255).toByte()
        putU16(27, 0)
        packet[29] = 0x3C.toByte()
        putU16(30, profile.cutoffTimerMin)
        putU16(32, profile.maxTimeMin)
        packet[34] = 0x00.toByte()
        packet[35] = chemistryCode.toByte()

        val secondaryVal = if (chemistryCode in listOf(0, 1, 2, 8, 9)) {
            profile.targetVoltageMv
        } else {
            3300
        }
        putU16(36, secondaryVal)

        var sum = 0
        for (i in 2..42) {
            sum += (packet[i].toInt() and 0xFF)
        }
        packet[43] = (sum and 0xFF).toByte()

        return packet
    }

    fun isConfigAckOk(packet: ByteArray): Boolean {
        if (packet.size < 6) return false
        if (packet[0] != 0x0F.toByte()) return false
        if (packet[1] != 0x04.toByte()) return false
        if (packet[2] != 0x94.toByte()) return false
        return packet[4] == 0x01.toByte()
    }

    fun parseStatus(packet: ByteArray): SlotStatus {
        val slotBitmask = readByte(packet, 3)
        val slot = when (slotBitmask) {
            1 -> 1
            2 -> 2
            4 -> 3
            8 -> 4
            else -> 0
        }
        val currentA = readUShort(packet, 4) / 1000f
        val voltageV = readUShort(packet, 6) / 1000f
        val rawTemp = readUShort(packet, 8) / 1000f
        val temperatureC = if (rawTemp < 1f) 0f else rawTemp
        val capacityMah = readUShort(packet, 10)
        val elapsedSeconds = readUInt(packet, 12)
        val internalResistanceMOhm = readUShort(packet, 16)
        val status = mapStatus(readByte(packet, 18))
        val mode = mapMode(readByte(packet, 19))
        val error = mapError(readByte(packet, 20))
        val chemistry = mapChemistry(readByte(packet, 21))

        return SlotStatus(
            slot = slot,
            voltageV = voltageV,
            currentA = currentA,
            temperatureC = temperatureC,
            capacityMah = capacityMah,
            elapsedSeconds = elapsedSeconds,
            internalResistanceMOhm = internalResistanceMOhm,
            status = status,
            mode = mode,
            error = error,
            chemistry = chemistry
        )
    }

    private fun readByte(packet: ByteArray, index: Int, default: Int = 0): Int {
        return if (index in packet.indices) {
            packet[index].toInt() and 0xFF
        } else {
            default
        }
    }

    private fun readUShort(packet: ByteArray, startIndex: Int, default: Int = 0): Int {
        if (startIndex + 1 < packet.size) {
            return ((packet[startIndex].toInt() and 0xFF) shl 8) or (packet[startIndex + 1].toInt() and 0xFF)
        }
        if (startIndex < packet.size) {
            return (packet[startIndex].toInt() and 0xFF) shl 8
        }
        return default
    }

    private fun readUInt(packet: ByteArray, startIndex: Int, default: Long = 0L): Long {
        if (startIndex >= packet.size) return default
        var result = 0L
        val availableBytes = minOf(4, packet.size - startIndex)
        for (i in 0 until availableBytes) {
            result = (result shl 8) or (packet[startIndex + i].toLong() and 0xFF)
        }
        return result
    }

    private fun mapChemistry(code: Int): String = when (code) {
        0 -> "Li-Ion"
        1 -> "Li-Ion HV"
        2 -> "LiFePO4"
        3 -> "NiMH"
        4 -> "NiCd"
        5 -> "Eneloop"
        6 -> "NiZn"
        7 -> "RAM"
        8 -> "LTO"
        9 -> "Na-Ion"
        else -> "Unknown"
    }

    private fun mapStatus(code: Int): String = when (code) {
        0 -> "Standby"
        1 -> "Processing"
        2 -> "Charging"
        3 -> "Discharging"
        4 -> "Resting"
        5, 6 -> "Completed"
        else -> "Unknown"
    }

    private fun mapMode(code: Int): String = when (code) {
        0 -> "Charge"
        1 -> "Discharge"
        2 -> "Storage"
        3 -> "Cycle"
        4 -> "Refresh"
        5 -> "Break_in"
        else -> "Mode $code"
    }

    private fun mapError(code: Int): String = when (code) {
        1 -> "Input voltage too low"
        2 -> "Input voltage too high"
        3 -> "Connection break"
        4 -> "Capacity limit reached"
        5 -> "Time limit reached"
        6 -> "Internal temperature too high"
        7 -> "Calibration failed"
        8 -> "High internal resistance"
        9 -> "Connection break"
        10 -> "Battery type error"
        11 -> "Overload protection"
        12 -> "Reversed polarity"
        13 -> "Fully charged"
        else -> ""
    }
}
