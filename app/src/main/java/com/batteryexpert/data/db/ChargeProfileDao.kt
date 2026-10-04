package com.batteryexpert.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeProfileDao {
    @Query("SELECT * FROM charge_profiles WHERE batteryId = :batteryId")
    fun getProfilesForBattery(batteryId: Long): Flow<List<ChargeProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ChargeProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ChargeProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ChargeProfileEntity)
}
