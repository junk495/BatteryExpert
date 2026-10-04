package com.batteryexpert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.assessment.AssessmentLogic
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.ui.viewmodels.BatteryDetailViewModel
import com.batteryexpert.ui.viewmodels.BatteryListViewModel

val CHEMISTRY_OPTIONS = listOf(
    "Li-Ion", "Li-Ion HV", "LiFePO4", "NiMH", "NiCd",
    "Eneloop", "NiZn", "RAM", "LTO", "Na-Ion"
)

val SIZE_OPTIONS = listOf(
    "18650", "21700", "AA", "AAA", "C", "D",
    "14500", "16340", "18350", "26650", "32700", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryEditScreen(
    batteryId: Long,
    listViewModel: BatteryListViewModel,
    detailViewModel: BatteryDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var chemistry by remember { mutableStateOf("Li-Ion") }
    var nominalVoltageV by remember { mutableStateOf("3.6") }
    var capacityMah by remember { mutableStateOf("3000") }
    var size by remember { mutableStateOf("18650") }
    var internalResistanceMOhm by remember { mutableStateOf("25") }
    var fastChargeCurrentMa by remember { mutableStateOf("3000") }
    var fastDischargeCurrentMa by remember { mutableStateOf("2000") }
    var slowChargeCurrentMa by remember { mutableStateOf("1500") }
    var slowDischargeCurrentMa by remember { mutableStateOf("600") }
    var deltaPeakMv by remember { mutableStateOf("") }
    var capacityCutoffMah by remember { mutableStateOf("") }
    var trickleChargeMa by remember { mutableStateOf("") }
    var keepVoltageMv by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isEditMode = batteryId > 0L

    LaunchedEffect(batteryId) {
        if (isEditMode) {
            detailViewModel.selectCellType(batteryId)
        }
    }

    val cellTypeToEdit by detailViewModel.cellType.collectAsState()

    LaunchedEffect(cellTypeToEdit) {
        cellTypeToEdit?.let { c ->
            manufacturer = c.manufacturer
            model = c.model
            chemistry = c.chemistry
            nominalVoltageV = c.nominalVoltageV.toString()
            capacityMah = c.nominalCapacityMah.toString()
            size = c.size
            internalResistanceMOhm = (c.typicalInternalResistanceMOhm ?: 25).toString()
            fastChargeCurrentMa = c.fastChargeCurrentMa.toString()
            fastDischargeCurrentMa = c.fastDischargeCurrentMa.toString()
            slowChargeCurrentMa = c.slowChargeCurrentMa.toString()
            slowDischargeCurrentMa = c.slowDischargeCurrentMa.toString()
            deltaPeakMv = c.deltaPeakMv?.toString() ?: ""
            capacityCutoffMah = c.capacityCutoffMah?.toString() ?: ""
            trickleChargeMa = c.trickleChargeMa?.toString() ?: ""
            keepVoltageMv = c.keepVoltageMv?.toString() ?: ""
            notes = c.notes
        }
    }

    fun updateCapacityAndDefaults(newCapStr: String) {
        capacityMah = newCapStr
        val cap = newCapStr.toIntOrNull() ?: 0
        if (cap > 0 && !isEditMode) {
            val defaults = AssessmentLogic.computeDefaults(cap)
            fastChargeCurrentMa = defaults.fastChargeMa.toString()
            fastDischargeCurrentMa = defaults.fastDischargeMa.toString()
            slowChargeCurrentMa = defaults.slowChargeMa.toString()
            slowDischargeCurrentMa = defaults.slowDischargeMa.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Zelltyp bearbeiten" else "Neuer Zelltyp") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = { Text("Hersteller (z.B. Panasonic)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modell (z.B. NCR18650B)") },
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
                value = capacityMah,
                onValueChange = { updateCapacityAndDefaults(it) },
                label = { Text("Nennkapazität (mAh)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = internalResistanceMOhm,
                onValueChange = { internalResistanceMOhm = it },
                label = { Text("Soll-Innenwiderstand (mΩ)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("Testströme für Zellbewertung", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fastChargeCurrentMa,
                    onValueChange = { fastChargeCurrentMa = it },
                    label = { Text("Schnell-Laden (mA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fastDischargeCurrentMa,
                    onValueChange = { fastDischargeCurrentMa = it },
                    label = { Text("Schnell-Entladen (mA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = slowChargeCurrentMa,
                    onValueChange = { slowChargeCurrentMa = it },
                    label = { Text("Genau-Laden (mA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = slowDischargeCurrentMa,
                    onValueChange = { slowDischargeCurrentMa = it },
                    label = { Text("Genau-Entladen (mA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

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
                label = { Text("Notizen (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abbrechen")
                }

                Button(
                    onClick = {
                        val cap = capacityMah.toIntOrNull() ?: 2000
                        val newCellType = CellTypeEntity(
                            id = if (isEditMode) batteryId else 0L,
                            manufacturer = manufacturer.ifBlank { "Unbekannt" },
                            model = model.ifBlank { "Unbekannt" },
                            chemistry = chemistry,
                            nominalVoltageV = nominalVoltageV.toFloatOrNull() ?: 3.6f,
                            nominalCapacityMah = cap,
                            size = size,
                            typicalInternalResistanceMOhm = internalResistanceMOhm.toIntOrNull() ?: 25,
                            fastChargeCurrentMa = fastChargeCurrentMa.toIntOrNull() ?: cap,
                            fastDischargeCurrentMa = fastDischargeCurrentMa.toIntOrNull() ?: minOf(cap, 2000),
                            slowChargeCurrentMa = slowChargeCurrentMa.toIntOrNull() ?: (cap * 0.5f).toInt(),
                            slowDischargeCurrentMa = slowDischargeCurrentMa.toIntOrNull() ?: (cap * 0.2f).toInt(),
                            deltaPeakMv = deltaPeakMv.toIntOrNull(),
                            capacityCutoffMah = capacityCutoffMah.toIntOrNull(),
                            trickleChargeMa = trickleChargeMa.toIntOrNull(),
                            keepVoltageMv = keepVoltageMv.toIntOrNull(),
                            notes = notes
                        )

                        if (isEditMode) {
                            listViewModel.updateCellType(newCellType)
                        } else {
                            listViewModel.insertCellType(newCellType)
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
