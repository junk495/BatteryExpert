package com.batteryexpert.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "batteries",
    foreignKeys = [
        ForeignKey(
            entity = CellTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["cellTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cellTypeId")]
)
data class BatteryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cellTypeId: Long,
    val label: String,
    val serialNumber: String? = null,
    val origin: String? = null,
    val purchaseDate: Long? = null,
    val purchaseCapacityMah: Int? = null,
    val purchaseInternalResistanceMOhm: Int? = null,
    val location: String? = null,
    val status: String = "NEW",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
