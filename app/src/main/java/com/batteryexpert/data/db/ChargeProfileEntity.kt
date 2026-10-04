package com.batteryexpert.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "charge_profiles",
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
data class ChargeProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batteryId: Long,
    val mode: String,
    val chargeCurrentMa: Int,
    val dischargeCurrentMa: Int,
    val targetVoltageMv: Int,
    val cutoffVoltageMv: Int,
    val terminationCurrentMa: Int,
    val cycleDirection: Int,
    val cycleCount: Int,
    val restChargeMin: Int,
    val restDischargeMin: Int,
    val trickleChargeMa: Int,
    val deltaPeakMv: Int,
    val cutoffTimerMin: Int,
    val maxTimeMin: Int
)
