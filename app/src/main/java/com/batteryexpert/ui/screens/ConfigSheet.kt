package com.batteryexpert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.assessment.AssessmentLogic
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileEntity
import java.util.Locale

fun chemistryCodeOf(chemistry: String): Int = when (chemistry.lowercase(Locale.GERMANY)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigSheet(
    cellTypes: List<CellTypeEntity>,
    onApply: (ChargeProfileEntity, Int, Int, Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedCellType by remember { mutableStateOf<CellTypeEntity?>(null) }
    var targetSlots by remember { mutableStateOf(setOf(1)) }

    var mode by remember { mutableStateOf("Charge") }
    var chargeCurrent by remember { mutableStateOf("1000") }
    var dischargeCurrent by remember { mutableStateOf("500") }
    var targetVoltageMv by remember { mutableStateOf("4200") }
    var cutoffVoltageMv by remember { mutableStateOf("3000") }
    var terminationCurrent by remember { mutableStateOf("100") }
    var deltaPeak by remember { mutableStateOf("0") }
    var capacityCutoff by remember { mutableStateOf("3000") }

    LaunchedEffect(selectedCellType) {
        val ct = selectedCellType ?: return@LaunchedEffect
        val defs = AssessmentLogic.computeDefaults(ct.nominalCapacityMah)
        mode = "Charge"
        chargeCurrent = (ct.chargeCurrentOptimalMa ?: defs.slowChargeMa).toString()
        dischargeCurrent = (ct.dischargeCurrentStandardMa ?: defs.slowDischargeMa).toString()
        targetVoltageMv = ((ct.chargeEndVoltageV ?: 4.2f) * 1000f).toInt().toString()
        cutoffVoltageMv = ((ct.dischargeCutoffRecommendedV ?: ct.dischargeCutoffAbsoluteMinV ?: 3.0f) * 1000f).toInt().toString()
        terminationCurrent = (ct.chargeTerminationCurrentMa ?: 100).toString()
        deltaPeak = (ct.deltaPeakMv ?: 0).toString()
        capacityCutoff = (ct.capacityCutoffMah ?: (ct.nominalCapacityMah * 1.1f).toInt()).toString()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                1 -> {
                    Text("Slot konfigurieren", style = MaterialTheme.typography.titleMedium)

                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                        OutlinedTextField(
                            value = selectedCellType?.let { "${it.manufacturer} ${it.model}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Zelltyp (optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            cellTypes.forEach { ct ->
                                DropdownMenuItem(
                                    text = { Text("${ct.manufacturer} ${ct.model}") },
                                    onClick = { selectedCellType = ct; typeExpanded = false }
                                )
                            }
                        }
                    }

                    var modeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
                        OutlinedTextField(
                            value = mode, onValueChange = {}, readOnly = true,
                            label = { Text("Modus") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                            listOf("Charge", "Storage", "Discharge", "Cycle", "Refresh", "Break-In").forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = { mode = m; modeExpanded = false })
                            }
                        }
                    }

                    OutlinedTextField(chargeCurrent, { chargeCurrent = it }, label = { Text("Ladestrom (mA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(dischargeCurrent, { dischargeCurrent = it }, label = { Text("Entladestrom (mA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(targetVoltageMv, { targetVoltageMv = it }, label = { Text("Ladeschlussspannung (mV)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cutoffVoltageMv, { cutoffVoltageMv = it }, label = { Text("Entladeschlussspannung (mV)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(terminationCurrent, { terminationCurrent = it }, label = { Text("Terminierungsstrom (mA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(deltaPeak, { deltaPeak = it }, label = { Text("Delta-Peak -dV (mV, NiMH)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(capacityCutoff, { capacityCutoff = it }, label = { Text("Kapazitäts-Cutoff (mAh)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Weiter") }
                }
                2 -> {
                    Text("Auf welche Slots anwenden?", style = MaterialTheme.typography.titleMedium)

                    val allSelected = targetSlots.size == 4
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = allSelected, onClick = { targetSlots = if (allSelected) emptySet() else setOf(1, 2, 3, 4) }, label = { Text("Alle") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 1..4) {
                            FilterChip(
                                selected = slot in targetSlots,
                                onClick = { targetSlots = if (slot in targetSlots) targetSlots - slot else targetSlots + slot },
                                label = { Text("Slot $slot") }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) { Text("Zurück") }
                        Button(onClick = {
                            val profile = ChargeProfileEntity(
                                id = 0,
                                batteryId = selectedCellType?.id ?: 0,
                                mode = mode,
                                chargeCurrentMa = chargeCurrent.toIntOrNull() ?: 1000,
                                dischargeCurrentMa = dischargeCurrent.toIntOrNull() ?: 500,
                                targetVoltageMv = targetVoltageMv.toIntOrNull() ?: 4200,
                                cutoffVoltageMv = cutoffVoltageMv.toIntOrNull() ?: 3000,
                                terminationCurrentMa = terminationCurrent.toIntOrNull() ?: 100,
                                cycleDirection = 0,
                                cycleCount = 1,
                                restChargeMin = 5,
                                restDischargeMin = 5,
                                trickleChargeMa = 0,
                                deltaPeakMv = deltaPeak.toIntOrNull() ?: 0,
                                cutoffTimerMin = 180,
                                maxTimeMin = 300
                            )
                            onApply(
                                profile,
                                chemistryCodeOf(selectedCellType?.chemistry ?: "Li-Ion"),
                                capacityCutoff.toIntOrNull() ?: 3000,
                                targetSlots
                            )
                            onDismiss()
                        }, modifier = Modifier.weight(1f)) { Text("Anwenden") }
                    }
                }
            }
        }
    }
}
