package com.batteryexpert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.ble.SlotStatus
import com.batteryexpert.ui.viewmodels.MonitorViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.util.Locale
import kotlin.math.round

private val NICE_STEPS = floatArrayOf(1f, 2f, 5f, 10f, 15f, 30f, 60f, 120f, 180f, 360f, 720f)

fun niceMinuteStep(totalMinutes: Float): Float {
    if (totalMinutes <= 0f) return 1f
    for (i in NICE_STEPS.size - 1 downTo 0) {
        val s = NICE_STEPS[i]
        val ticks = totalMinutes / s
        if (ticks >= 4f && ticks <= 10f) return s
    }
    return NICE_STEPS.first()
}

fun formatAxisMinutes(minutes: Float): String {
    val totalMin = round(minutes).toInt()
    if (totalMin < 60) return "$totalMin min"
    val h = totalMin / 60
    val m = totalMin % 60
    return if (m == 0) "${h} h" else "${h} h ${m} min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val slotStatuses by viewModel.slotStatuses.collectAsState()
    val slotHistories by viewModel.slotHistories.collectAsState()
    val cellTypes by viewModel.cellTypes.collectAsState()

    var configSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MC5000 Monitor") }
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

            Text(
                text = "Akku-Slots (1-4)",
                style = MaterialTheme.typography.titleLarge
            )

            for (slotIndex in 1..4) {
                val status = slotStatuses.firstOrNull { it.slot == slotIndex }
                val history = slotHistories[slotIndex]
                val tHistory = history?.timestamps ?: emptyList()
                val vHistory = history?.voltages ?: emptyList()
                val cHistory = history?.currents ?: emptyList()

                SlotCard(
                    slotNumber = slotIndex,
                    slotStatus = status,
                    voltageHistory = vHistory,
                    currentHistory = cHistory,
                    timestamps = tHistory,
                    onConfigure = { configSheetVisible = true }
                )
            }

            if (configSheetVisible) {
                ConfigSheet(
                    cellTypes = cellTypes,
                    onApply = { profile, chem, cap, slots ->
                        viewModel.sendConfig(profile, chem, cap, slots)
                    },
                    onDismiss = { configSheetVisible = false }
                )
            }
        }
    }
}

@Composable
fun SlotCard(
    slotNumber: Int,
    slotStatus: SlotStatus?,
    voltageHistory: List<Float>,
    currentHistory: List<Float>,
    timestamps: List<Long>,
    onConfigure: () -> Unit
) {
    val statusText = slotStatus?.status ?: "Standby"
    val statusColor = getStatusColor(statusText)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Slot $slotNumber",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onConfigure) {
                        Icon(Icons.Default.Settings, contentDescription = "Konfigurieren")
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            HorizontalDivider()

            if (slotStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ValueColumn(label = "Spannung", value = "%.2f V".format(Locale.GERMANY, slotStatus.voltageV))
                    ValueColumn(label = "Strom", value = "%.2f A".format(Locale.GERMANY, slotStatus.currentA))
                    ValueColumn(label = "Temp.", value = "%.1f °C".format(Locale.GERMANY, slotStatus.temperatureC))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ValueColumn(label = "Kapazität", value = "${slotStatus.capacityMah} mAh")
                    ValueColumn(label = "Zeit", value = formatTime(slotStatus.elapsedSeconds))
                    ValueColumn(label = "Innenwiderstand", value = "${slotStatus.internalResistanceMOhm} mΩ")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ValueColumn(label = "Modus", value = slotStatus.mode)
                    ValueColumn(label = "Chemie", value = slotStatus.chemistry)
                }

                if (slotStatus.error.isNotBlank()) {
                    Text(
                        text = "Fehler: ${slotStatus.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (voltageHistory.size >= 2 && voltageHistory.any { it > 0f }) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Spannungs- & Stromverlauf (Live)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val firstTimestamp = timestamps.firstOrNull() ?: 0L
                    val lastTimestamp = timestamps.lastOrNull() ?: firstTimestamp
                    val totalMinutes = (lastTimestamp - firstTimestamp) / 60_000f
                    val step = niceMinuteStep(totalMinutes)

                    val voltageEntries = voltageHistory.mapIndexed { idx, v ->
                        val rawMin = if (idx in timestamps.indices) (timestamps[idx] - firstTimestamp) / 60_000f else idx.toFloat()
                        val minutes = round(rawMin * 100f) / 100f
                        entryOf(minutes, v)
                    }
                    val currentEntries = currentHistory.mapIndexed { idx, a ->
                        val rawMin = if (idx in timestamps.indices) (timestamps[idx] - firstTimestamp) / 60_000f else idx.toFloat()
                        val minutes = round(rawMin * 100f) / 100f
                        entryOf(minutes, a)
                    }

                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(lineColor = Color(0xFF1E88E5)),
                                lineSpec(lineColor = Color(0xFFE53935))
                            )
                        ),
                        model = entryModelOf(voltageEntries, currentEntries),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ -> formatAxisMinutes(value) }
                        ),
                        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
                        getXStep = { step },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "● Spannung (V)",
                            color = Color(0xFF1E88E5),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "● Strom (A)",
                            color = Color(0xFFE53935),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                Text(
                    text = "Keine Daten empfangen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ValueColumn(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun getStatusColor(status: String): Color {
    return when (status.lowercase(Locale.GERMANY)) {
        "charging", "laden" -> Color(0xFFE53935)
        "discharging", "entladen" -> Color(0xFF1E88E5)
        "completed", "fertig" -> Color(0xFF43A047)
        "standby" -> Color(0xFF757575)
        "processing", "verarbeiten" -> Color(0xFFFB8C00)
        "resting", "ruhe" -> Color(0xFF8E24AA)
        else -> Color(0xFF757575)
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.GERMANY, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.GERMANY, "%02d:%02d", m, s)
    }
}
