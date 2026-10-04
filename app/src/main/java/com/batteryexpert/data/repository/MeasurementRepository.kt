package com.batteryexpert.data.repository

import com.batteryexpert.data.db.MeasurementDao
import com.batteryexpert.data.db.MeasurementEntity
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val measurementDao: MeasurementDao
) {
    fun getMeasurementsForBattery(batteryId: Long): Flow<List<MeasurementEntity>> =
        measurementDao.getMeasurementsForBattery(batteryId)

    suspend fun insertMeasurements(measurements: List<MeasurementEntity>) =
        measurementDao.insertMeasurements(measurements)

    suspend fun deleteMeasurementsForBattery(batteryId: Long) =
        measurementDao.deleteMeasurementsForBattery(batteryId)
}
