package com.batteryexpert.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppDatabaseTest {

    @Test
    fun testCellTypeEntityInstantiation() {
        val cellType = CellTypeEntity(
            id = 1,
            manufacturer = "Panasonic",
            model = "NCR18650B",
            chemistry = "Li-Ion",
            nominalVoltageV = 3.6f,
            nominalCapacityMah = 3400,
            size = "18650",
            typicalInternalResistanceMOhm = 35,
            fastChargeCurrentMa = 3400,
            fastDischargeCurrentMa = 2000,
            slowChargeCurrentMa = 1700,
            slowDischargeCurrentMa = 680
        )

        assertEquals(1L, cellType.id)
        assertEquals("Panasonic", cellType.manufacturer)
        assertEquals("NCR18650B", cellType.model)
        assertEquals("Li-Ion", cellType.chemistry)
        assertEquals(3.6f, cellType.nominalVoltageV, 0.01f)
        assertEquals(3400, cellType.nominalCapacityMah)
        assertEquals(3400, cellType.fastChargeCurrentMa)
    }

    @Test
    fun testBatteryEntityInstantiation() {
        val battery = BatteryEntity(
            id = 1,
            cellTypeId = 1,
            label = "Akku #01",
            status = "NEW"
        )

        assertEquals(1L, battery.id)
        assertEquals(1L, battery.cellTypeId)
        assertEquals("Akku #01", battery.label)
        assertEquals("NEW", battery.status)
    }

    @Test
    fun testChargeProfileEntityInstantiation() {
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

        assertEquals(1L, profile.id)
        assertEquals(1L, profile.batteryId)
        assertEquals("Charge", profile.mode)
        assertEquals(1000, profile.chargeCurrentMa)
    }

    @Test
    fun testMeasurementEntityInstantiation() {
        val measurement = MeasurementEntity(
            id = 1,
            batteryId = 1,
            slot = 1,
            timestamp = System.currentTimeMillis(),
            voltageV = 4.15f,
            currentA = 1.0f,
            temperatureC = 28.5f,
            capacityMah = 1200,
            internalResistanceMOhm = 32,
            phase = "Charging",
            status = "Processing"
        )

        assertNotNull(measurement)
        assertEquals(1L, measurement.batteryId)
        assertEquals(1, measurement.slot)
        assertEquals(4.15f, measurement.voltageV, 0.01f)
    }

    @Test
    fun testTestResultEntityInstantiation() {
        val result = TestResultEntity(
            id = 1,
            batteryId = 1,
            slot = 1,
            timestamp = System.currentTimeMillis(),
            testType = "FAST",
            chargeCurrentMa = 3000,
            dischargeCurrentMa = 2000,
            cutoffVoltageMv = 3000,
            measuredCapacityMah = 2850,
            internalResistanceMOhm = 28,
            sohPercent = 95.0f,
            recommendation = "OK",
            note = "Zelle in gutem Zustand"
        )

        assertNotNull(result)
        assertEquals(1L, result.batteryId)
        assertEquals("FAST", result.testType)
        assertEquals(95.0f, result.sohPercent, 0.01f)
        assertEquals("OK", result.recommendation)
    }
}
