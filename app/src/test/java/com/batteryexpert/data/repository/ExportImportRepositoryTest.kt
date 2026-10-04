package com.batteryexpert.data.repository

import com.batteryexpert.data.db.BatteryDao
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeDao
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileDao
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.db.MeasurementDao
import com.batteryexpert.data.db.MeasurementEntity
import com.batteryexpert.data.db.TestResultDao
import com.batteryexpert.data.db.TestResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportImportRepositoryTest {

    private val fakeCellType = CellTypeEntity(
        id = 1,
        manufacturer = "Panasonic",
        model = "NCR18650B",
        chemistry = "Li-Ion",
        nominalVoltageV = 3.6f,
        nominalCapacityMah = 3400,
        size = "18650",
        typicalInternalResistanceMOhm = 35
    )

    private val fakeBattery = BatteryEntity(
        id = 1,
        cellTypeId = 1,
        label = "Akku #01"
    )

    private val fakeCellTypeDao = object : CellTypeDao {
        private val list = MutableStateFlow(listOf(fakeCellType))
        override fun getAllCellTypes(): Flow<List<CellTypeEntity>> = list
        override fun getCellTypeById(id: Long): Flow<CellTypeEntity?> = flowOf(fakeCellType)
        override suspend fun insertCellType(cellType: CellTypeEntity): Long = 10L
        override suspend fun updateCellType(cellType: CellTypeEntity) {}
        override suspend fun deleteCellType(cellType: CellTypeEntity) {}
    }

    private val fakeBatteryDao = object : BatteryDao {
        override fun getAllBatteries(): Flow<List<BatteryEntity>> = flowOf(listOf(fakeBattery))
        override fun getBatteryById(id: Long): Flow<BatteryEntity?> = flowOf(fakeBattery)
        override fun getBatteriesForType(cellTypeId: Long): Flow<List<BatteryEntity>> = flowOf(listOf(fakeBattery))
        override suspend fun insertBattery(battery: BatteryEntity): Long = 100L
        override suspend fun updateBattery(battery: BatteryEntity) {}
        override suspend fun deleteBattery(battery: BatteryEntity) {}
    }

    private val fakeChargeProfileDao = object : ChargeProfileDao {
        override fun getProfilesForBattery(batteryId: Long): Flow<List<ChargeProfileEntity>> = flowOf(emptyList())
        override suspend fun insertProfile(profile: ChargeProfileEntity): Long = 20L
        override suspend fun updateProfile(profile: ChargeProfileEntity) {}
        override suspend fun deleteProfile(profile: ChargeProfileEntity) {}
    }

    private val fakeMeasurementDao = object : MeasurementDao {
        override fun getMeasurementsForBattery(batteryId: Long): Flow<List<MeasurementEntity>> = flowOf(emptyList())
        override suspend fun insertMeasurements(measurements: List<MeasurementEntity>) {}
        override suspend fun deleteMeasurementsForBattery(batteryId: Long) {}
    }

    private val fakeTestResultDao = object : TestResultDao {
        override fun getResultsForBattery(batteryId: Long): Flow<List<TestResultEntity>> = flowOf(emptyList())
        override fun getAll(): Flow<List<TestResultEntity>> = flowOf(emptyList())
        override suspend fun insert(result: TestResultEntity): Long = 1L
        override suspend fun delete(result: TestResultEntity) {}
    }

    private val repository = ExportImportRepository(
        cellTypeDao = fakeCellTypeDao,
        batteryDao = fakeBatteryDao,
        chargeProfileDao = fakeChargeProfileDao,
        measurementDao = fakeMeasurementDao,
        testResultDao = fakeTestResultDao
    )

    @Test
    fun testExportAndImportCycle() = runBlocking {
        val exportedJson = repository.exportDataToJson()
        assertTrue(exportedJson.contains("Panasonic"))
        assertTrue(exportedJson.contains("NCR18650B"))

        val importResult = repository.importDataFromJson(exportedJson)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.getOrNull())
    }
}
