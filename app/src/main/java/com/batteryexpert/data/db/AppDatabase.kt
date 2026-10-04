package com.batteryexpert.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// Hinweis: Für die Entwicklungsphase wird fallbackToDestructiveMigration() verwendet.
// Bei Schema-Änderungen gehen gespeicherte Daten verloren.

@Database(
    entities = [
        CellTypeEntity::class,
        BatteryEntity::class,
        ChargeProfileEntity::class,
        MeasurementEntity::class,
        TestResultEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cellTypeDao(): CellTypeDao
    abstract fun batteryDao(): BatteryDao
    abstract fun chargeProfileDao(): ChargeProfileDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun testResultDao(): TestResultDao
}
