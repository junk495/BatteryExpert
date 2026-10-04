package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.assessment.AssessmentLogic
import com.batteryexpert.data.assessment.Recommendation
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.ble.ProtocolCodec
import com.batteryexpert.data.ble.SlotStatus
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.db.TestResultEntity
import com.batteryexpert.data.repository.BleRepository
import com.batteryexpert.data.repository.TestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TestType(val label: String) {
    FAST("Schnelltest (~3 h)"),
    SLOW("Genauer Test (~5-6 h)")
}

data class SlotTestConfig(
    val slot: Int,
    val cellTypeId: Long? = null,
    val batteryId: Long? = null,
    val testType: TestType = TestType.FAST,
    val measuredIR: Int? = null,
    val irRecommendation: Recommendation? = null,
    val testSaved: Boolean = false
)

class TestViewModel(
    private val testRepository: TestRepository,
    private val bleRepository: BleRepository,
    private val protocolCodec: ProtocolCodec = ProtocolCodec()
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState

    val cellTypes: StateFlow<List<CellTypeEntity>> = testRepository.observeCellTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val batteries: StateFlow<List<BatteryEntity>> = testRepository.observeBatteries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _slotConfigs = MutableStateFlow<Map<Int, SlotTestConfig>>(
        mapOf(
            1 to SlotTestConfig(1),
            2 to SlotTestConfig(2),
            3 to SlotTestConfig(3),
            4 to SlotTestConfig(4)
        )
    )
    val slotConfigs: StateFlow<Map<Int, SlotTestConfig>> = _slotConfigs.asStateFlow()

    private val _selectedBatteryForResults = MutableStateFlow<Long?>(null)
    val selectedBatteryForResults: StateFlow<Long?> = _selectedBatteryForResults.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedBatteryResults: StateFlow<List<TestResultEntity>> = _selectedBatteryForResults.flatMapLatest { id ->
        if (id != null) testRepository.observeResults(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _slotStatuses = MutableStateFlow<List<SlotStatus>>(emptyList())
    val slotStatuses: StateFlow<List<SlotStatus>> = _slotStatuses.asStateFlow()

    init {
        observeStatusesAndAutoSave()
    }

    fun setSlotCellType(slot: Int, cellTypeId: Long?) {
        val current = _slotConfigs.value.toMutableMap()
        val existing = current[slot] ?: SlotTestConfig(slot)
        current[slot] = existing.copy(cellTypeId = cellTypeId, measuredIR = null, irRecommendation = null, testSaved = false)
        _slotConfigs.value = current
    }

    fun setSlotTestType(slot: Int, testType: TestType) {
        val current = _slotConfigs.value.toMutableMap()
        val existing = current[slot] ?: SlotTestConfig(slot)
        current[slot] = existing.copy(testType = testType)
        _slotConfigs.value = current
    }

    fun selectBatteryForResults(batteryId: Long?) {
        _selectedBatteryForResults.value = batteryId
    }

    fun checkSlotIR(slot: Int) {
        val status = _slotStatuses.value.firstOrNull { it.slot == slot }
        val config = _slotConfigs.value[slot] ?: return
        val cellType = cellTypes.value.firstOrNull { it.id == config.cellTypeId } ?: return

        val measured = if (status != null && status.internalResistanceMOhm > 0) status.internalResistanceMOhm else 25
        val target = cellType.typicalInternalResistanceMOhm ?: 25
        val rec = AssessmentLogic.irRecommendation(measured, target)

        val current = _slotConfigs.value.toMutableMap()
        current[slot] = config.copy(measuredIR = measured, irRecommendation = rec)
        _slotConfigs.value = current
    }

    fun startTests() {
        viewModelScope.launch {
            val configs = _slotConfigs.value
            configs.forEach { (slot, config) ->
                val cellTypeId = config.cellTypeId ?: return@forEach
                val cellType = cellTypes.value.firstOrNull { it.id == cellTypeId } ?: return@forEach

                val defaults = AssessmentLogic.computeDefaults(cellType.nominalCapacityMah)
                val (chargeMa, dischargeMa) = when (config.testType) {
                    TestType.FAST -> {
                        val c = if (cellType.fastChargeCurrentMa > 0) cellType.fastChargeCurrentMa else defaults.fastChargeMa
                        val d = if (cellType.fastDischargeCurrentMa > 0) cellType.fastDischargeCurrentMa else defaults.fastDischargeMa
                        c to d
                    }
                    TestType.SLOW -> {
                        val c = if (cellType.slowChargeCurrentMa > 0) cellType.slowChargeCurrentMa else defaults.slowChargeMa
                        val d = if (cellType.slowDischargeCurrentMa > 0) cellType.slowDischargeCurrentMa else defaults.slowDischargeMa
                        c to d
                    }
                }

                val cutoffMv = when (cellType.chemistry.lowercase()) {
                    "li-ion" -> 3000
                    "li-ion hv" -> 3200
                    "lifepo4" -> 2800
                    "nimh", "nicd", "eneloop" -> 900
                    "nizn" -> 1100
                    "lto" -> 1800
                    "na-ion" -> 2000
                    else -> 3000
                }

                val targetMv = when (cellType.chemistry.lowercase()) {
                    "li-ion" -> 4200
                    "li-ion hv" -> 4350
                    "lifepo4" -> 3650
                    "nimh", "nicd", "eneloop" -> 1650
                    "nizn" -> 1900
                    "lto" -> 2850
                    "na-ion" -> 4000
                    else -> 4200
                }

                val profile = ChargeProfileEntity(
                    id = 0,
                    batteryId = cellType.id,
                    mode = "Cycle",
                    chargeCurrentMa = chargeMa,
                    dischargeCurrentMa = dischargeMa,
                    targetVoltageMv = targetMv,
                    cutoffVoltageMv = cutoffMv,
                    terminationCurrentMa = (cellType.nominalCapacityMah * 0.05f).toInt().coerceAtLeast(30),
                    cycleDirection = 0,
                    cycleCount = 1,
                    restChargeMin = 10,
                    restDischargeMin = 10,
                    trickleChargeMa = cellType.trickleChargeMa ?: 0,
                    deltaPeakMv = cellType.deltaPeakMv ?: 6,
                    cutoffTimerMin = 360,
                    maxTimeMin = 480
                )

                val chemistryCode = when (cellType.chemistry.lowercase()) {
                    "li-ion" -> 0
                    "li-ion hv" -> 1
                    "lifepo4" -> 2
                    "nimh" -> 3
                    "nicd" -> 4
                    "eneloop" -> 5
                    "nizn" -> 6
                    "ram" -> 7
                    "lto" -> 8
                    "na-ion" -> 9
                    else -> 0
                }

                val bitmask = when (slot) {
                    1 -> 1
                    2 -> 2
                    3 -> 4
                    4 -> 8
                    else -> 1
                }

                val capacityCutoff = cellType.capacityCutoffMah ?: (cellType.nominalCapacityMah * 1.1f).toInt()

                try {
                    val configPacket = protocolCodec.buildChargeConfig(
                        profile = profile,
                        chemistryCode = chemistryCode,
                        slotBitmask = bitmask,
                        capacityCutoffMah = capacityCutoff
                    )
                    bleRepository.writePacket(configPacket)
                    bleRepository.startStop(bitmask)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopTests() {
        viewModelScope.launch {
            bleRepository.startStop(0)
        }
    }

    private fun observeStatusesAndAutoSave() {
        viewModelScope.launch {
            bleRepository.observeSlotStatuses().collect { status ->
                val currentStatuses = _slotStatuses.value.toMutableList()
                currentStatuses.removeAll { it.slot == status.slot }
                currentStatuses.add(status)
                _slotStatuses.value = currentStatuses

                if (status.status.equals("completed", ignoreCase = true)) {
                    val config = _slotConfigs.value[status.slot] ?: return@collect
                    val cellTypeId = config.cellTypeId ?: return@collect
                    if (!config.testSaved) {
                        val cellType = cellTypes.value.firstOrNull { it.id == cellTypeId } ?: return@collect
                        val (soh, rec) = AssessmentLogic.sohRecommendation(
                            measuredCapacityMah = status.capacityMah,
                            ratedCapacityMah = cellType.nominalCapacityMah
                        )

                        val defaults = AssessmentLogic.computeDefaults(cellType.nominalCapacityMah)
                        val (cMa, dMa) = when (config.testType) {
                            TestType.FAST -> (if (cellType.fastChargeCurrentMa > 0) cellType.fastChargeCurrentMa else defaults.fastChargeMa) to (if (cellType.fastDischargeCurrentMa > 0) cellType.fastDischargeCurrentMa else defaults.fastDischargeMa)
                            TestType.SLOW -> (if (cellType.slowChargeCurrentMa > 0) cellType.slowChargeCurrentMa else defaults.slowChargeMa) to (if (cellType.slowDischargeCurrentMa > 0) cellType.slowDischargeCurrentMa else defaults.slowDischargeMa)
                        }

                        val testResult = TestResultEntity(
                            id = 0L,
                            batteryId = cellType.id,
                            slot = status.slot,
                            timestamp = System.currentTimeMillis(),
                            testType = config.testType.name,
                            chargeCurrentMa = cMa,
                            dischargeCurrentMa = dMa,
                            cutoffVoltageMv = 3000,
                            measuredCapacityMah = status.capacityMah,
                            internalResistanceMOhm = status.internalResistanceMOhm,
                            sohPercent = soh,
                            recommendation = rec.name,
                            note = "Automatisch gespeichert nach Test-Abschluss"
                        )

                        testRepository.saveResult(testResult)

                        val updatedConfigs = _slotConfigs.value.toMutableMap()
                        updatedConfigs[status.slot] = config.copy(testSaved = true)
                        _slotConfigs.value = updatedConfigs
                    }
                }
            }
        }
    }
}
