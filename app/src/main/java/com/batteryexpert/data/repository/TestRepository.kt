package com.batteryexpert.data.repository

import com.batteryexpert.data.db.BatteryDao
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeDao
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.TestResultDao
import com.batteryexpert.data.db.TestResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TestRepository(
    private val cellTypeDao: CellTypeDao,
    private val batteryDao: BatteryDao,
    private val testResultDao: TestResultDao
) {
    fun observeCellTypes(): Flow<List<CellTypeEntity>> = cellTypeDao.getAllCellTypes()

    fun observeBatteries(): Flow<List<BatteryEntity>> = batteryDao.getAllBatteries()

    suspend fun saveResult(result: TestResultEntity) = testResultDao.insert(result)

    fun observeResults(batteryId: Long): Flow<List<TestResultEntity>> =
        testResultDao.getResultsForBattery(batteryId)

    suspend fun getCellType(cellTypeId: Long): CellTypeEntity? =
        cellTypeDao.getCellTypeById(cellTypeId).firstOrNull()
}
