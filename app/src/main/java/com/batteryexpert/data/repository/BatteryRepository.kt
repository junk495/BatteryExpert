package com.batteryexpert.data.repository

import com.batteryexpert.data.db.BatteryDao
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeDao
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileDao
import com.batteryexpert.data.db.ChargeProfileEntity
import kotlinx.coroutines.flow.Flow

class BatteryRepository(
    private val cellTypeDao: CellTypeDao,
    private val batteryDao: BatteryDao,
    private val chargeProfileDao: ChargeProfileDao
) {
    fun getCellTypes(): Flow<List<CellTypeEntity>> = cellTypeDao.getAllCellTypes()

    fun getCellTypeById(id: Long): Flow<CellTypeEntity?> = cellTypeDao.getCellTypeById(id)

    suspend fun insertCellType(cellType: CellTypeEntity): Long = cellTypeDao.insertCellType(cellType)

    suspend fun updateCellType(cellType: CellTypeEntity) = cellTypeDao.updateCellType(cellType)

    suspend fun deleteCellType(cellType: CellTypeEntity) = cellTypeDao.deleteCellType(cellType)

    fun getBatteriesForType(cellTypeId: Long): Flow<List<BatteryEntity>> =
        batteryDao.getBatteriesForType(cellTypeId)

    fun getAllBatteries(): Flow<List<BatteryEntity>> = batteryDao.getAllBatteries()

    fun getBatteryById(id: Long): Flow<BatteryEntity?> = batteryDao.getBatteryById(id)

    suspend fun insertBattery(battery: BatteryEntity): Long = batteryDao.insertBattery(battery)

    suspend fun updateBattery(battery: BatteryEntity) = batteryDao.updateBattery(battery)

    suspend fun deleteBattery(battery: BatteryEntity) = batteryDao.deleteBattery(battery)

    fun getProfilesForBattery(batteryId: Long): Flow<List<ChargeProfileEntity>> =
        chargeProfileDao.getProfilesForBattery(batteryId)

    suspend fun insertProfile(profile: ChargeProfileEntity): Long = chargeProfileDao.insertProfile(profile)

    suspend fun updateProfile(profile: ChargeProfileEntity) = chargeProfileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: ChargeProfileEntity) = chargeProfileDao.deleteProfile(profile)
}
