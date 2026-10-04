package com.batteryexpert.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = BatteryEntity::class,
            parentColumns = ["id"],
            childColumns = ["batteryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("batteryId")]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batteryId: Long,
    val slot: Int,
    val timestamp: Long,
    val voltageV: Float,
    val currentA: Float,
    val temperatureC: Float,
    val capacityMah: Int,
    val internalResistanceMOhm: Int,
    val phase: String,
    val status: String
)
