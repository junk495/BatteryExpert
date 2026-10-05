package com.batteryexpert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.batteryexpert.data.ApiKeyStore
import com.batteryexpert.data.ble.Mc5000BleManager
import com.batteryexpert.data.db.AppDatabase
import com.batteryexpert.data.repository.AiRepository
import com.batteryexpert.data.repository.BatteryRepository
import com.batteryexpert.data.repository.BleRepository
import com.batteryexpert.data.repository.ExportImportRepository
import com.batteryexpert.data.repository.MeasurementRepository
import com.batteryexpert.data.repository.TestRepository
import com.batteryexpert.ui.screens.AiResearchScreen
import com.batteryexpert.ui.screens.BatteryDetailScreen
import com.batteryexpert.ui.screens.BatteryEditScreen
import com.batteryexpert.ui.screens.BatteryListScreen
import com.batteryexpert.ui.screens.MonitorScreen
import com.batteryexpert.ui.screens.SettingsScreen
import com.batteryexpert.ui.test.TestScreen
import com.batteryexpert.ui.theme.BatteryExpertTheme
import com.batteryexpert.ui.viewmodels.AiResearchViewModel
import com.batteryexpert.ui.viewmodels.BatteryDetailViewModel
import com.batteryexpert.ui.viewmodels.BatteryListViewModel
import com.batteryexpert.ui.viewmodels.MonitorViewModel
import com.batteryexpert.ui.viewmodels.SettingsViewModel
import com.batteryexpert.ui.viewmodels.TestViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object BatteryList : Screen("battery_list", "Akkus", Icons.AutoMirrored.Filled.List)
    data object Monitor : Screen("monitor", "Monitor", Icons.Default.Info)
    data object Test : Screen("test", "Test", Icons.Default.Search)
    data object Settings : Screen("settings", "Einstellungen", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "battery_expert.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val apiKeyStore by lazy { ApiKeyStore(applicationContext) }
    private val aiRepo by lazy { AiRepository(apiKeyStore) }

    private val batteryRepo by lazy { BatteryRepository(db.cellTypeDao(), db.batteryDao(), db.chargeProfileDao()) }
    private val measurementRepo by lazy { MeasurementRepository(db.measurementDao()) }
    private val exportImportRepo by lazy { ExportImportRepository(db.cellTypeDao(), db.batteryDao(), db.chargeProfileDao(), db.measurementDao(), db.testResultDao()) }
    private val testRepo by lazy { TestRepository(db.cellTypeDao(), db.batteryDao(), db.testResultDao()) }

    private val bleManager by lazy { Mc5000BleManager(applicationContext) }
    private val bleRepo by lazy { BleRepository(bleManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryExpertTheme {
                MainApp(
                    batteryRepo = batteryRepo,
                    measurementRepo = measurementRepo,
                    exportImportRepo = exportImportRepo,
                    testRepo = testRepo,
                    bleRepo = bleRepo,
                    apiKeyStore = apiKeyStore,
                    aiRepo = aiRepo
                )
            }
        }
    }
}

@Composable
fun MainApp(
    batteryRepo: BatteryRepository,
    measurementRepo: MeasurementRepository,
    exportImportRepo: ExportImportRepository,
    testRepo: TestRepository,
    bleRepo: BleRepository,
    apiKeyStore: ApiKeyStore,
    aiRepo: AiRepository
) {
    val listViewModel: BatteryListViewModel = viewModel { BatteryListViewModel(batteryRepo) }
    val detailViewModel: BatteryDetailViewModel = viewModel { BatteryDetailViewModel(batteryRepo, measurementRepo) }
    val monitorViewModel: MonitorViewModel = viewModel { MonitorViewModel(bleRepo) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(exportImportRepo, bleRepo, apiKeyStore) }
    val testViewModel: TestViewModel = viewModel { TestViewModel(testRepo, bleRepo) }
    val aiResearchViewModel: AiResearchViewModel = viewModel { AiResearchViewModel(aiRepo) }

    val navController = rememberNavController()
    val bottomNavScreens = listOf(
        Screen.BatteryList,
        Screen.Monitor,
        Screen.Test,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.BatteryList.route,
        Screen.Monitor.route,
        Screen.Test.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.BatteryList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.BatteryList.route) {
                BatteryListScreen(
                    viewModel = listViewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate("battery_detail/$id")
                    },
                    onNavigateToEdit = { id ->
                        navController.navigate("battery_edit/$id")
                    },
                    onNavigateToAiResearch = {
                        navController.navigate("ai_research")
                    }
                )
            }

            composable("ai_research") {
                AiResearchScreen(
                    aiViewModel = aiResearchViewModel,
                    listViewModel = listViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "battery_detail/{batteryId}",
                arguments = listOf(navArgument("batteryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val batteryId = backStackEntry.arguments?.getLong("batteryId") ?: -1L
                BatteryDetailScreen(
                    batteryId = batteryId,
                    detailViewModel = detailViewModel,
                    monitorViewModel = monitorViewModel,
                    onNavigateToEdit = { id ->
                        navController.navigate("battery_edit/$id")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "battery_edit/{batteryId}",
                arguments = listOf(navArgument("batteryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val batteryId = backStackEntry.arguments?.getLong("batteryId") ?: -1L
                BatteryEditScreen(
                    batteryId = batteryId,
                    listViewModel = listViewModel,
                    detailViewModel = detailViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Monitor.route) {
                MonitorScreen(viewModel = monitorViewModel)
            }

            composable(Screen.Test.route) {
                TestScreen(viewModel = testViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
