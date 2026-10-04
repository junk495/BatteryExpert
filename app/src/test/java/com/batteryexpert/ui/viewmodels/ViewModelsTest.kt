package com.batteryexpert.ui.viewmodels

import com.batteryexpert.data.db.BatteryDao
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeDao
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileDao
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.db.MeasurementDao
import com.batteryexpert.data.db.MeasurementEntity
import com.batteryexpert.data.repository.BatteryRepository
import com.batteryexpert.data.repository.MeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeCellType = CellTypeEntity(
        id = 1,
        manufacturer = "Sony",
        model = "VTC6",
        chemistry = "Li-Ion",
        nominalVoltageV = 3.6f,
        nominalCapacityMah = 3000,
        size = "18650",
        typicalInternalResistanceMOhm = 12
    )

    private val fakeCellTypeDao = object : CellTypeDao {
        private val list = MutableStateFlow(listOf(fakeCellType))
        override fun getAllCellTypes(): Flow<List<CellTypeEntity>> = list
        override fun getCellTypeById(id: Long): Flow<CellTypeEntity?> = flowOf(fakeCellType)
        override suspend fun insertCellType(cellType: CellTypeEntity): Long = cellType.id
        override suspend fun updateCellType(cellType: CellTypeEntity) {}
        override suspend fun deleteCellType(cellType: CellTypeEntity) {}
    }

    private val fakeBatteryDao = object : BatteryDao {
        override fun getAllBatteries(): Flow<List<BatteryEntity>> = flowOf(emptyList())
        override fun getBatteryById(id: Long): Flow<BatteryEntity?> = flowOf(null)
        override fun getBatteriesForType(cellTypeId: Long): Flow<List<BatteryEntity>> = flowOf(emptyList())
        override suspend fun insertBattery(battery: BatteryEntity): Long = battery.id
        override suspend fun updateBattery(battery: BatteryEntity) {}
        override suspend fun deleteBattery(battery: BatteryEntity) {}
    }

    private val fakeChargeProfileDao = object : ChargeProfileDao {
        override fun getProfilesForBattery(batteryId: Long): Flow<List<ChargeProfileEntity>> = flowOf(emptyList())
        override suspend fun insertProfile(profile: ChargeProfileEntity): Long = profile.id
        override suspend fun updateProfile(profile: ChargeProfileEntity) {}
        override suspend fun deleteProfile(profile: ChargeProfileEntity) {}
    }

    private val fakeMeasurementDao = object : MeasurementDao {
        override fun getMeasurementsForBattery(batteryId: Long): Flow<List<MeasurementEntity>> = flowOf(emptyList())
        override suspend fun insertMeasurements(measurements: List<MeasurementEntity>) {}
        override suspend fun deleteMeasurementsForBattery(batteryId: Long) {}
    }

    private val batteryRepository = BatteryRepository(fakeCellTypeDao, fakeBatteryDao, fakeChargeProfileDao)
    private val measurementRepository = MeasurementRepository(fakeMeasurementDao)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBatteryListViewModel_filteringAndSorting() = runTest(testDispatcher) {
        val viewModel = BatteryListViewModel(batteryRepository)
        testScheduler.advanceUntilIdle()

        val initialList = viewModel.cellTypes.value
        assertEquals(1, initialList.size)
        assertEquals("Sony", initialList[0].manufacturer)

        viewModel.setSearchQuery("NonExistent")
        testScheduler.advanceUntilIdle()
        assertEquals(0, viewModel.cellTypes.value.size)

        viewModel.setSearchQuery("Sony")
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.cellTypes.value.size)
    }

    @Test
    fun testBatteryDetailViewModel_selection() = runTest(testDispatcher) {
        val viewModel = BatteryDetailViewModel(batteryRepository, measurementRepository)
        viewModel.selectCellType(1L)
        testScheduler.advanceUntilIdle()

        assertEquals(1L, viewModel.selectedCellTypeId.value)
        assertEquals("Sony", viewModel.cellType.value?.manufacturer)
    }
}
