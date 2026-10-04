package com.batteryexpert.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE batteryId = :batteryId ORDER BY timestamp ASC")
    fun getMeasurementsForBattery(batteryId: Long): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE batteryId = :batteryId")
    suspend fun deleteMeasurementsForBattery(batteryId: Long)
}
