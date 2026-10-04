package com.batteryexpert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.ui.viewmodels.BatteryDetailViewModel
import com.batteryexpert.ui.viewmodels.MonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDetailScreen(
    batteryId: Long,
    detailViewModel: BatteryDetailViewModel,
    monitorViewModel: MonitorViewModel,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(batteryId) {
        detailViewModel.selectCellType(batteryId)
    }

    val cellType by detailViewModel.cellType.collectAsState()
    val concreteBatteries by detailViewModel.batteries.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var cellCountText by remember { mutableStateOf("1") }

    val currentCellTypeForDialog = cellType
    if (showDeleteDialog && currentCellTypeForDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Zelltyp löschen?") },
            text = { Text("Möchtest du den Zelltyp '${currentCellTypeForDialog.manufacturer} ${currentCellTypeForDialog.model}' wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        detailViewModel.deleteCellType(currentCellTypeForDialog)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cellType?.let { "${it.manufacturer} ${it.model}" } ?: "Zelltyp-Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(batteryId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val currentCellType = cellType
        if (currentCellType == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Zelltyp nicht gefunden", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Stammdaten", style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Hersteller: ${currentCellType.manufacturer}")
                        Text("Modell: ${currentCellType.model}")
                        if (!currentCellType.aliases.isNullOrBlank()) Text("Aliase: ${currentCellType.aliases}")
                        Text("Chemie: ${currentCellType.chemistry}")
                        Text("Größe: ${currentCellType.size}")
                        Text("Nennspannung: ${currentCellType.nominalVoltageV} V")
                        Text("Nennkapazität: ${currentCellType.nominalCapacityMah} mAh")
                        currentCellType.nominalEnergyWh?.let { Text("Nennenergie: $it Wh") }
                        currentCellType.typicalInternalResistanceMOhm?.let { Text("Soll-Innenwiderstand: $it mΩ") }
                        if (currentCellType.notes.isNotBlank()) Text("Notizen: ${currentCellType.notes}")
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Testströme (Bewertung)", style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Schnelltest Laden: ${currentCellType.fastChargeCurrentMa} mA")
                        Text("Schnelltest Entladen: ${currentCellType.fastDischargeCurrentMa} mA")
                        Text("Genauer Test Laden: ${currentCellType.slowChargeCurrentMa} mA")
                        Text("Genauer Test Entladen: ${currentCellType.slowDischargeCurrentMa} mA")
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
                        Text("Konkrete Einzelzellen (${concreteBatteries.size})", style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = cellCountText,
                                onValueChange = { cellCountText = it },
                                label = { Text("Anzahl") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp)
                            )

                            Button(
                                onClick = {
                                    val count = cellCountText.toIntOrNull()?.coerceIn(1, 50) ?: 1
                                    val startIndex = concreteBatteries.size + 1
                                    for (i in 0 until count) {
                                        val labelStr = "${currentCellType.model} #${startIndex + i}"
                                        detailViewModel.insertBattery(
                                            BatteryEntity(
                                                id = 0L,
                                                cellTypeId = currentCellType.id,
                                                label = labelStr,
                                                status = "NEW"
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Zellen anlegen")
                            }
                        }

                        if (concreteBatteries.isEmpty()) {
                            Text("Noch keine konkreten Einzelzellen angelegt.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            concreteBatteries.forEach { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${b.label} (${b.status})", style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { detailViewModel.deleteBattery(b) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
