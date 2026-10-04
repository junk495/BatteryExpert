package com.batteryexpert.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattery(battery: BatteryEntity): Long

    @Update
    suspend fun updateBattery(battery: BatteryEntity)

    @Delete
    suspend fun deleteBattery(battery: BatteryEntity)

    @Query("SELECT * FROM batteries ORDER BY createdAt DESC")
    fun getAllBatteries(): Flow<List<BatteryEntity>>

    @Query("SELECT * FROM batteries WHERE id = :id")
    fun getBatteryById(id: Long): Flow<BatteryEntity?>

    @Query("SELECT * FROM batteries WHERE cellTypeId = :cellTypeId ORDER BY label ASC")
    fun getBatteriesForType(cellTypeId: Long): Flow<List<BatteryEntity>>
}
