package com.batteryexpert.data.repository

import com.batteryexpert.data.db.BatteryDao
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeDao
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileDao
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.db.MeasurementDao
import com.batteryexpert.data.db.MeasurementEntity
import com.batteryexpert.data.db.TestResultDao
import com.batteryexpert.data.db.TestResultEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FullExportData(
    val version: Int = 4,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val cellTypes: List<CellTypeEntity> = emptyList(),
    val batteries: List<BatteryEntity> = emptyList(),
    val profiles: List<ChargeProfileEntity> = emptyList(),
    val testResults: List<TestResultEntity> = emptyList()
)

class ExportImportRepository(
    private val cellTypeDao: CellTypeDao,
    private val batteryDao: BatteryDao,
    private val chargeProfileDao: ChargeProfileDao,
    private val measurementDao: MeasurementDao,
    private val testResultDao: TestResultDao,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val cellTypes = cellTypeDao.getAllCellTypes().first()
        val batteries = batteryDao.getAllBatteries().first()
        val testResults = testResultDao.getAll().first()

        val fullData = FullExportData(
            version = 4,
            exportTimestamp = System.currentTimeMillis(),
            cellTypes = cellTypes,
            batteries = batteries,
            profiles = emptyList(),
            testResults = testResults
        )
        gson.toJson(fullData)
    }

    suspend fun exportTestResultsToCsv(): String = withContext(Dispatchers.IO) {
        val results = testResultDao.getAll().first()
        val sb = StringBuilder()
        sb.append("ID;Datum;Slot;Testart;Ladestrom (mA);Entladestrom (mA);Abschaltspannung (mV);Gemessene Kapazitaet (mAh);Innenwiderstand (mOhm);SOH (%);Empfehlung;Notiz\n")

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY)

        results.forEach { r ->
            val dateStr = dateFormat.format(Date(r.timestamp))
            val line = listOf(
                r.id.toString(),
                dateStr,
                r.slot.toString(),
                r.testType,
                r.chargeCurrentMa.toString(),
                r.dischargeCurrentMa.toString(),
                r.cutoffVoltageMv.toString(),
                r.measuredCapacityMah.toString(),
                r.internalResistanceMOhm.toString(),
                "%.2f".format(Locale.GERMANY, r.sohPercent),
                r.recommendation,
                (r.note ?: "").replace(";", ",")
            ).joinToString(";")
            sb.append(line).append("\n")
        }

        sb.toString()
    }

    suspend fun importDataFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fullData = gson.fromJson(jsonString, FullExportData::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Ungültiges JSON-Format."))

            var importedCount = 0
            val cellTypeIdMap = mutableMapOf<Long, Long>()

            fullData.cellTypes.forEach { cellType ->
                val oldId = cellType.id
                val newId = cellTypeDao.insertCellType(cellType.copy(id = 0L))
                cellTypeIdMap[oldId] = newId
                importedCount++
            }

            fullData.batteries.forEach { battery ->
                val newCellTypeId = cellTypeIdMap[battery.cellTypeId] ?: battery.cellTypeId
                batteryDao.insertBattery(battery.copy(id = 0L, cellTypeId = newCellTypeId))
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
