package com.batteryexpert.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity): Long

    @Query("SELECT * FROM test_results WHERE batteryId = :batteryId ORDER BY timestamp DESC")
    fun getResultsForBattery(batteryId: Long): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TestResultEntity>>

    @Delete
    suspend fun delete(result: TestResultEntity)
}
