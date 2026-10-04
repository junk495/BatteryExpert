package com.batteryexpert.data.ble

import com.batteryexpert.data.db.ChargeProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {

    private val codec = ProtocolCodec()

    @Test
    fun testBuildPacket_emptyData() {
        val packet = codec.buildPacket(0x91, byteArrayOf())
        assertEquals(4, packet.size)
        assertEquals(0x0F.toByte(), packet[0])
        assertEquals(2.toByte(), packet[1])
        assertEquals(0x91.toByte(), packet[2])
        assertEquals(0x91.toByte(), packet[3])
    }

    @Test
    fun testBuildPacket_withData() {
        val data = byteArrayOf(0x01, 0x02)
        val packet = codec.buildPacket(0x93, data)
        assertEquals(6, packet.size)
        assertEquals(0x0F.toByte(), packet[0])
        assertEquals(4.toByte(), packet[1])
        assertEquals(0x93.toByte(), packet[2])
        assertEquals(0x01.toByte(), packet[3])
        assertEquals(0x02.toByte(), packet[4])
        assertEquals(0x96.toByte(), packet[5])
    }

    @Test
    fun testChecksumValid_validPackets() {
        val packet1 = codec.buildPacket(0x91, byteArrayOf(0x0F))
        assertTrue(codec.checksumValid(packet1))

        val packet2 = codec.buildStartStop(3)
        assertTrue(codec.checksumValid(packet2))

        val packet3 = codec.buildStatusRequest(1)
        assertTrue(codec.checksumValid(packet3))
    }

    @Test
    fun testChecksumValid_invalidPackets() {
        val badHeader = byteArrayOf(0x00, 0x02, 0x91.toByte(), 0x91.toByte())
        assertFalse(codec.checksumValid(badHeader))

        val tooShort = byteArrayOf(0x0F, 0x02, 0x91.toByte())
        assertFalse(codec.checksumValid(tooShort))

        val validPacket = codec.buildPacket(0x91, byteArrayOf(0x05))
        validPacket[validPacket.lastIndex] = 0x00
        assertFalse(codec.checksumValid(validPacket))

        val corruptLength = byteArrayOf(0x0F, 0x20, 0x91.toByte(), 0x91.toByte())
        assertFalse(codec.checksumValid(corruptLength))
    }

    @Test
    fun testBuildStatusRequestAndStartStop() {
        val statusReq = codec.buildStatusRequest(0x0F)
        assertEquals(0x0F.toByte(), statusReq[0])
        assertEquals(0x91.toByte(), statusReq[2])
        assertEquals(0x0F.toByte(), statusReq[3])

        val startStop = codec.buildStartStop(2)
        assertEquals(0x0F.toByte(), startStop[0])
        assertEquals(0x93.toByte(), startStop[2])
        assertEquals(0x02.toByte(), startStop[3])
    }

    @Test
    fun testBuildChargeConfig() {
        val profile = ChargeProfileEntity(
            id = 1,
            batteryId = 1,
            mode = "Charge",
            chargeCurrentMa = 1000,
            dischargeCurrentMa = 500,
            targetVoltageMv = 4200,
            cutoffVoltageMv = 3000,
            terminationCurrentMa = 50,
            cycleDirection = 0,
            cycleCount = 1,
            restChargeMin = 5,
            restDischargeMin = 5,
            trickleChargeMa = 0,
            deltaPeakMv = 0,
            cutoffTimerMin = 180,
            maxTimeMin = 240
        )

        val packet = codec.buildChargeConfig(profile, chemistryCode = 0, slotBitmask = 1)

        assertEquals(44, packet.size)
        assertEquals(0x0F.toByte(), packet[0])
        assertEquals(0x2A.toByte(), packet[1])
        assertEquals(0x94.toByte(), packet[2])
        assertEquals(0x01.toByte(), packet[3])
        assertEquals(0x00.toByte(), packet[4])

        assertEquals(0x03.toByte(), packet[5])
        assertEquals(0xE8.toByte(), packet[6])

        assertEquals(0x10.toByte(), packet[11])
        assertEquals(0x68.toByte(), packet[12])

        assertEquals(0x00.toByte(), packet[35])
    }

    @Test
    fun testIsConfigAckOk() {
        val validAck = byteArrayOf(0x0F, 0x04, 0x94.toByte(), 0x01, 0x01, 0x96.toByte())
        assertTrue(codec.isConfigAckOk(validAck))

        val badAck = byteArrayOf(0x0F, 0x04, 0x94.toByte(), 0x01, 0x00, 0x95.toByte())
        assertFalse(codec.isConfigAckOk(badAck))
    }

    @Test
    fun testParseStatus_fullPacket() {
        val packet = ByteArray(22)
        packet[0] = 0x0F.toByte()
        packet[1] = 20.toByte()
        packet[2] = 0x91.toByte()
        packet[3] = 1.toByte()

        packet[4] = 0x05.toByte()
        packet[5] = 0xDC.toByte()

        packet[6] = 0x10.toByte()
        packet[7] = 0x68.toByte()

        packet[8] = 0x61.toByte()
        packet[9] = 0xA8.toByte()

        packet[10] = 0x09.toByte()
        packet[11] = 0xC4.toByte()

        packet[12] = 0x00.toByte()
        packet[13] = 0x00.toByte()
        packet[14] = 0x0E.toByte()
        packet[15] = 0x10.toByte()

        packet[16] = 0x00.toByte()
        packet[17] = 0x0F.toByte()

        packet[18] = 2.toByte()
        packet[19] = 0.toByte()
        packet[20] = 0.toByte()
        packet[21] = 0.toByte()

        val status = codec.parseStatus(packet)

        assertEquals(1, status.slot)
        assertEquals(1.5f, status.currentA, 0.001f)
        assertEquals(4.2f, status.voltageV, 0.001f)
        assertEquals(25.0f, status.temperatureC, 0.001f)
        assertEquals(2500, status.capacityMah)
        assertEquals(3600L, status.elapsedSeconds)
        assertEquals(15, status.internalResistanceMOhm)
        assertEquals("Charging", status.status)
        assertEquals("Charge", status.mode)
        assertEquals("", status.error)
        assertEquals("Li-Ion", status.chemistry)
    }
}
