package com.batteryexpert.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.assessment.Recommendation
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.TestResultEntity
import com.batteryexpert.ui.screens.ConfigDropdownSelector
import com.batteryexpert.ui.viewmodels.SlotTestConfig
import com.batteryexpert.ui.viewmodels.TestType
import com.batteryexpert.ui.viewmodels.TestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    viewModel: TestViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val cellTypes by viewModel.cellTypes.collectAsState()
    val slotConfigs by viewModel.slotConfigs.collectAsState()
    val slotStatuses by viewModel.slotStatuses.collectAsState()
    val selectedBatteryResults by viewModel.selectedBatteryResults.collectAsState()
    val selectedBatteryId by viewModel.selectedBatteryForResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zell-Alterungstest") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (connectionState != ConnectionState.CONNECTED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nicht mit MC5000 verbunden. Bitte in den Einstellungen verbinden.",
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text("Slot-Zuordnung & Testeinstellungen", style = MaterialTheme.typography.titleLarge)

            for (slot in 1..4) {
                val config = slotConfigs[slot] ?: SlotTestConfig(slot)
                val status = slotStatuses.firstOrNull { it.slot == slot }

                SlotTestCard(
                    slot = slot,
                    config = config,
                    cellTypes = cellTypes,
                    slotStatus = status,
                    onSelectCellType = { cellTypeId -> viewModel.setSlotCellType(slot, cellTypeId) },
                    onSelectTestType = { testType -> viewModel.setSlotTestType(slot, testType) },
                    onCheckIR = { viewModel.checkSlotIR(slot) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.startTests() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tests starten")
                }

                OutlinedButton(
                    onClick = { viewModel.stopTests() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Abbrechen")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Testergebnisse & SOH-Verlauf", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    if (cellTypes.isNotEmpty()) {
                        val selectedName = cellTypes.firstOrNull { it.id == selectedBatteryId }?.let { "${it.manufacturer} ${it.model}" } ?: ""
                        ConfigDropdownSelector(
                            label = "Ergebnisse filtern nach Zelltyp",
                            options = cellTypes.map { "${it.manufacturer} ${it.model}" },
                            selectedOption = selectedName,
                            onOptionSelected = { name ->
                                val found = cellTypes.firstOrNull { "${it.manufacturer} ${it.model}" == name }
                                viewModel.selectBatteryForResults(found?.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (selectedBatteryResults.isEmpty()) {
                        Text(
                            text = "Keine vergangenen Testergebnisse für diesen Zelltyp vorhanden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    } else {
                        selectedBatteryResults.forEach { result ->
                            TestResultRow(result = result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlotTestCard(
    slot: Int,
    config: SlotTestConfig,
    cellTypes: List<CellTypeEntity>,
    slotStatus: com.batteryexpert.data.ble.SlotStatus?,
    onSelectCellType: (Long?) -> Unit,
    onSelectTestType: (TestType) -> Unit,
    onCheckIR: () -> Unit
) {
    val assignedCellType = cellTypes.firstOrNull { it.id == config.cellTypeId }
    val options = listOf("Keine Zelle") + cellTypes.map { "${it.manufacturer} ${it.model} (${it.chemistry})" }
    val currentSelection = assignedCellType?.let { "${it.manufacturer} ${it.model} (${it.chemistry})" } ?: "Keine Zelle"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Slot $slot", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()

            ConfigDropdownSelector(
                label = "Zelltyp aus Datenbank zuordnen",
                options = options,
                selectedOption = currentSelection,
                onOptionSelected = { selected ->
                    if (selected == "Keine Zelle") {
                        onSelectCellType(null)
                    } else {
                        val found = cellTypes.firstOrNull { "${it.manufacturer} ${it.model} (${it.chemistry})" == selected }
                        onSelectCellType(found?.id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (assignedCellType != null) {
                Text("Testart:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TestType.entries.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.testType == type,
                                onClick = { onSelectTestType(type) }
                            )
                            Text(type.label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onCheckIR) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("IR prüfen")
                    }

                    if (config.measuredIR != null && config.irRecommendation != null) {
                        val recColor = when (config.irRecommendation) {
                            Recommendation.OK -> Color(0xFF43A047)
                            Recommendation.WATCH -> Color(0xFFFB8C00)
                            Recommendation.SORT_OUT -> Color(0xFFE53935)
                        }

                        Surface(
                            color = recColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(recColor, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "IR: ${config.measuredIR} mΩ (${config.irRecommendation.label})",
                                    color = recColor,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                if (slotStatus != null) {
                    Text(
                        text = "Live: ${slotStatus.voltageV} V | ${slotStatus.currentA} A | ${slotStatus.capacityMah} mAh | ${slotStatus.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TestResultRow(result: TestResultEntity) {
    val recColor = when (result.recommendation.uppercase()) {
        "OK" -> Color(0xFF43A047)
        "WATCH", "BEOBACHTEN" -> Color(0xFFFB8C00)
        else -> Color(0xFFE53935)
    }

    val recLabel = when (result.recommendation.uppercase()) {
        "OK" -> "OK"
        "WATCH", "BEOBACHTEN" -> "Beobachten"
        else -> "Aussortieren"
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
    val dateStr = dateFormat.format(Date(result.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dateStr • ${result.testType}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Kapazität: ${result.measuredCapacityMah} mAh | SOH: %.1f %%".format(Locale.GERMANY, result.sohPercent),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "IR: ${result.internalResistanceMOhm} mΩ | Strom: ${result.chargeCurrentMa}/${result.dischargeCurrentMa} mA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = recColor.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Text(
                    text = recLabel,
                    color = recColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
