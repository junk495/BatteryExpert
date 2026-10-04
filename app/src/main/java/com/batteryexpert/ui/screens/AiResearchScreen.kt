package com.batteryexpert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.ui.viewmodels.AiResearchState
import com.batteryexpert.ui.viewmodels.AiResearchViewModel
import com.batteryexpert.ui.viewmodels.BatteryListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiResearchScreen(
    aiViewModel: AiResearchViewModel,
    listViewModel: BatteryListViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by aiViewModel.query.collectAsState()
    val state by aiViewModel.state.collectAsState()

    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var chemistry by remember { mutableStateOf("Li-Ion") }
    var nominalVoltageV by remember { mutableStateOf("3.6") }
    var nominalCapacityMah by remember { mutableStateOf("3000") }
    var size by remember { mutableStateOf("18650") }
    var typicalInternalResistanceMOhm by remember { mutableStateOf("25") }
    var deltaPeakMv by remember { mutableStateOf("") }
    var capacityCutoffMah by remember { mutableStateOf("") }
    var trickleChargeMa by remember { mutableStateOf("") }
    var keepVoltageMv by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        val currentState = state
        if (currentState is AiResearchState.Success) {
            val c = currentState.cellType
            manufacturer = c.manufacturer
            model = c.model
            chemistry = c.chemistry
            nominalVoltageV = c.nominalVoltageV.toString()
            nominalCapacityMah = c.nominalCapacityMah.toString()
            size = c.size
            typicalInternalResistanceMOhm = (c.typicalInternalResistanceMOhm ?: 25).toString()
            deltaPeakMv = c.deltaPeakMv?.toString() ?: if (c.chemistry in listOf("NiMH", "NiCd", "Eneloop")) "3" else ""
            capacityCutoffMah = c.capacityCutoffMah?.toString() ?: if (c.chemistry in listOf("NiMH", "NiCd", "Eneloop")) (c.nominalCapacityMah * 1.15f).toInt().toString() else ""
            trickleChargeMa = c.trickleChargeMa?.toString() ?: if (c.chemistry in listOf("NiMH", "NiCd", "Eneloop")) (c.nominalCapacityMah * 0.03f).toInt().coerceAtLeast(30).toString() else ""
            keepVoltageMv = c.keepVoltageMv?.toString() ?: if (c.chemistry in listOf("NiMH", "NiCd", "Eneloop")) "1350" else ""
            notes = c.notes.ifBlank { "KI-Recherche Daten" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KI-Akku Recherche") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Akkudaten via KI ermitteln",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { aiViewModel.setQuery(it) },
                        label = { Text("Akkumodell / Suchbegriff (z.B. 'Eneloop AA' oder 'Samsung 30Q')") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { aiViewModel.research() },
                        enabled = query.isNotBlank() && state !is AiResearchState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state is AiResearchState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recherchiere...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recherchieren")
                        }
                    }
                }
            }

            val currentState = state
            if (currentState is AiResearchState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fehler: ${currentState.message}",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (currentState is AiResearchState.Success) {
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
                        Text(
                            text = "Gefundene Daten überprüfen & speichern",
                            style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider()

                        OutlinedTextField(
                            value = manufacturer,
                            onValueChange = { manufacturer = it },
                            label = { Text("Hersteller") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Modell") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownSelector(
                            label = "Chemie",
                            options = CHEMISTRY_OPTIONS,
                            selectedOption = chemistry,
                            onOptionSelected = { chemistry = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownSelector(
                            label = "Größe",
                            options = SIZE_OPTIONS,
                            selectedOption = size,
                            onOptionSelected = { size = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = nominalVoltageV,
                            onValueChange = { nominalVoltageV = it },
                            label = { Text("Nennspannung (V)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = nominalCapacityMah,
                            onValueChange = { nominalCapacityMah = it },
                            label = { Text("Nennkapazität (mAh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = typicalInternalResistanceMOhm,
                            onValueChange = { typicalInternalResistanceMOhm = it },
                            label = { Text("Typischer Innenwiderstand (mΩ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (chemistry in listOf("NiMH", "NiCd", "Eneloop")) {
                            HorizontalDivider()
                            Text("NiMH-Ladeparameter", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = deltaPeakMv,
                                onValueChange = { deltaPeakMv = it },
                                label = { Text("Delta-Peak -dV (mV)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = capacityCutoffMah,
                                onValueChange = { capacityCutoffMah = it },
                                label = { Text("Kapazitäts-Cutoff (mAh)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = trickleChargeMa,
                                onValueChange = { trickleChargeMa = it },
                                label = { Text("Erhaltungsladung (mA)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = keepVoltageMv,
                                onValueChange = { keepVoltageMv = it },
                                label = { Text("Erhaltungsspannung (mV)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notizen") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { aiViewModel.resetState() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verwerfen")
                            }

                            Button(
                                onClick = {
                                    val cap = nominalCapacityMah.toIntOrNull() ?: 2000
                                    val defaults = com.batteryexpert.data.assessment.AssessmentLogic.computeDefaults(cap)
                                    val newCellType = CellTypeEntity(
                                        id = 0L,
                                        manufacturer = manufacturer.ifBlank { "Unbekannt" },
                                        model = model.ifBlank { "Unbekannt" },
                                        chemistry = chemistry,
                                        nominalVoltageV = nominalVoltageV.toFloatOrNull() ?: 3.6f,
                                        nominalCapacityMah = cap,
                                        size = size,
                                        typicalInternalResistanceMOhm = typicalInternalResistanceMOhm.toIntOrNull() ?: 25,
                                        fastChargeCurrentMa = defaults.fastChargeMa,
                                        fastDischargeCurrentMa = defaults.fastDischargeMa,
                                        slowChargeCurrentMa = defaults.slowChargeMa,
                                        slowDischargeCurrentMa = defaults.slowDischargeMa,
                                        deltaPeakMv = deltaPeakMv.toIntOrNull(),
                                        capacityCutoffMah = capacityCutoffMah.toIntOrNull(),
                                        trickleChargeMa = trickleChargeMa.toIntOrNull(),
                                        keepVoltageMv = keepVoltageMv.toIntOrNull(),
                                        notes = notes.ifBlank { "KI-Recherche Daten" }
                                    )
                                    listViewModel.insertCellType(newCellType)
                                    aiViewModel.resetState()
                                    onNavigateBack()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Speichern")
                            }
                        }
                    }
                }
            }
        }
    }
}
