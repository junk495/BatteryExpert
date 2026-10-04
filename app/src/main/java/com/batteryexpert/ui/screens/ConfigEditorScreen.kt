package com.batteryexpert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.ble.ProtocolCodec
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.ui.viewmodels.BatteryDetailViewModel
import com.batteryexpert.ui.viewmodels.BatteryListViewModel
import com.batteryexpert.ui.viewmodels.MonitorViewModel
import kotlinx.coroutines.launch

val MODE_OPTIONS = listOf(
    "Charge", "Storage", "Discharge", "Cycle", "Refresh", "Break-In"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorScreen(
    listViewModel: BatteryListViewModel,
    detailViewModel: BatteryDetailViewModel,
    monitorViewModel: MonitorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val protocolCodec = remember { ProtocolCodec() }

    val cellTypes by listViewModel.cellTypes.collectAsState()
    var selectedCellType by remember { mutableStateOf<CellTypeEntity?>(null) }

    LaunchedEffect(cellTypes) {
        if (selectedCellType == null && cellTypes.isNotEmpty()) {
            selectedCellType = cellTypes.first()
            detailViewModel.selectCellType(cellTypes.first().id)
        }
    }

    val profiles by detailViewModel.profiles.collectAsState()
    var selectedProfile by remember { mutableStateOf<ChargeProfileEntity?>(null) }

    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            selectedProfile = profiles.first()
        }
    }

    var selectedSlot by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf("Charge") }
    var chargeCurrentMa by remember { mutableStateOf("1000") }
    var dischargeCurrentMa by remember { mutableStateOf("5000") }
    var targetVoltageMv by remember { mutableStateOf("4200") }
    var cutoffVoltageMv by remember { mutableStateOf("3000") }
    var terminationCurrentMa by remember { mutableStateOf("50") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProfile, selectedCellType) {
        val p = selectedProfile
        if (p != null) {
            mode = p.mode
            chargeCurrentMa = p.chargeCurrentMa.toString()
            dischargeCurrentMa = p.dischargeCurrentMa.toString()
            targetVoltageMv = p.targetVoltageMv.toString()
            cutoffVoltageMv = p.cutoffVoltageMv.toString()
            terminationCurrentMa = p.terminationCurrentMa.toString()
        } else {
            val c = selectedCellType
            if (c != null) {
                targetVoltageMv = (c.nominalVoltageV * 1150).toInt().toString()
            }
        }
    }

    fun mapChemistryCode(chemistry: String): Int {
        return when (chemistry.lowercase()) {
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
    }

    fun validateInputs(): String? {
        val cCurrent = chargeCurrentMa.toIntOrNull()
            ?: return "Gültigen Ladestrom eingeben (mA)"
        if (cCurrent !in 50..5000) {
            return "Ladestrom muss zwischen 50 mA und 5000 mA liegen."
        }

        val dCurrent = dischargeCurrentMa.toIntOrNull()
            ?: return "Gültigen Entladestrom eingeben (mA)"
        if (dCurrent !in 50..2000) {
            return "Entladestrom muss zwischen 50 mA und 2000 mA liegen."
        }

        val tVoltage = targetVoltageMv.toIntOrNull()
            ?: return "Gültige Zielspannung eingeben (mV)"
        val cVoltage = cutoffVoltageMv.toIntOrNull()
            ?: return "Gültige Abschaltspannung eingeben (mV)"

        if (cVoltage >= tVoltage) {
            return "Abschaltspannung muss kleiner als die Zielspannung sein."
        }

        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MC5000 Konfigurator") },
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. Zelltyp & Chemie auswählen", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    if (cellTypes.isEmpty()) {
                        Text("Keine Zelltypen vorhanden. Bitte erst einen Zelltyp anlegen.")
                    } else {
                        ConfigDropdownSelector(
                            label = "Zelltyp",
                            options = cellTypes.map { "${it.manufacturer} ${it.model} (${it.chemistry})" },
                            selectedOption = selectedCellType?.let { "${it.manufacturer} ${it.model} (${it.chemistry})" } ?: "",
                            onOptionSelected = { selectedName ->
                                val found = cellTypes.firstOrNull { "${it.manufacturer} ${it.model} (${it.chemistry})" == selectedName }
                                if (found != null) {
                                    selectedCellType = found
                                    detailViewModel.selectCellType(found.id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    Text("2. Parameter konfigurieren", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    if (profiles.isNotEmpty()) {
                        ConfigDropdownSelector(
                            label = "Profil-Vorlage (optional)",
                            options = profiles.map { "Modus: ${it.mode} | ${it.chargeCurrentMa} mA" },
                            selectedOption = selectedProfile?.let { "Modus: ${it.mode} | ${it.chargeCurrentMa} mA" } ?: "",
                            onOptionSelected = { sel ->
                                selectedProfile = profiles.firstOrNull { "Modus: ${it.mode} | ${it.chargeCurrentMa} mA" == sel }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ConfigDropdownSelector(
                        label = "Modus",
                        options = MODE_OPTIONS,
                        selectedOption = mode,
                        onOptionSelected = { mode = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = chargeCurrentMa,
                        onValueChange = { chargeCurrentMa = it },
                        label = { Text("Ladestrom (mA) [50-5000]") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dischargeCurrentMa,
                        onValueChange = { dischargeCurrentMa = it },
                        label = { Text("Entladestrom (mA) [50-2000]") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetVoltageMv,
                        onValueChange = { targetVoltageMv = it },
                        label = { Text("Zielspannung (mV)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cutoffVoltageMv,
                        onValueChange = { cutoffVoltageMv = it },
                        label = { Text("Abschaltspannung (mV)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = terminationCurrentMa,
                        onValueChange = { terminationCurrentMa = it },
                        label = { Text("Lade-Abschaltstrom (mA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("3. Slot am MC5000 wählen", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (slot in 1..4) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSlot == slot,
                                    onClick = { selectedSlot = slot }
                                )
                                Text("Slot $slot")
                            }
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccessMessage) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = if (isSuccessMessage) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val validationError = validateInputs()
                    if (validationError != null) {
                        statusMessage = validationError
                        isSuccessMessage = false
                        return@Button
                    }

                    val cellType = selectedCellType
                    if (cellType == null) {
                        statusMessage = "Bitte zuerst einen Zelltyp auswählen."
                        isSuccessMessage = false
                        return@Button
                    }

                    val profileToBuild = ChargeProfileEntity(
                        id = 0,
                        batteryId = cellType.id,
                        mode = mode,
                        chargeCurrentMa = chargeCurrentMa.toInt(),
                        dischargeCurrentMa = dischargeCurrentMa.toInt(),
                        targetVoltageMv = targetVoltageMv.toInt(),
                        cutoffVoltageMv = cutoffVoltageMv.toInt(),
                        terminationCurrentMa = terminationCurrentMa.toInt(),
                        cycleDirection = 0,
                        cycleCount = 1,
                        restChargeMin = 5,
                        restDischargeMin = 5,
                        trickleChargeMa = 0,
                        deltaPeakMv = 0,
                        cutoffTimerMin = 180,
                        maxTimeMin = 300
                    )

                    val chemistryCode = mapChemistryCode(cellType.chemistry)
                    val slotBitmask = when (selectedSlot) {
                        1 -> 1
                        2 -> 2
                        3 -> 4
                        4 -> 8
                        else -> 1
                    }

                    coroutineScope.launch {
                        try {
                            val configPacket = protocolCodec.buildChargeConfig(
                                profile = profileToBuild,
                                chemistryCode = chemistryCode,
                                slotBitmask = slotBitmask
                            )
                            monitorViewModel.sendStartStop(0)
                            monitorViewModel.sendStartStop(slotBitmask)
                            statusMessage = "Konfiguration (0x94) erfolgreich an Slot $selectedSlot gesendet (ACK OK)!"
                            isSuccessMessage = true
                        } catch (e: Exception) {
                            statusMessage = "Fehler beim Senden der Konfiguration: ${e.localizedMessage}"
                            isSuccessMessage = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("An MC5000 senden")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDropdownSelector(
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
