package com.batteryexpert.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_results",
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
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batteryId: Long,
    val slot: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val testType: String, // "IR_ONLY", "FAST", "SLOW"
    val chargeCurrentMa: Int,
    val dischargeCurrentMa: Int,
    val cutoffVoltageMv: Int,
    val measuredCapacityMah: Int,
    val internalResistanceMOhm: Int,
    val sohPercent: Float,
    val recommendation: String, // "OK", "WATCH", "SORT_OUT"
    val note: String? = null
)
