package com.batteryexpert.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CellTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCellType(cellType: CellTypeEntity): Long

    @Update
    suspend fun updateCellType(cellType: CellTypeEntity)

    @Delete
    suspend fun deleteCellType(cellType: CellTypeEntity)

    @Query("SELECT * FROM cell_types ORDER BY manufacturer ASC, model ASC")
    fun getAllCellTypes(): Flow<List<CellTypeEntity>>

    @Query("SELECT * FROM cell_types WHERE id = :id")
    fun getCellTypeById(id: Long): Flow<CellTypeEntity?>
}
