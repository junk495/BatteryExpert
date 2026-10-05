package com.batteryexpert.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.batteryexpert.data.ApiKeyStore.Provider
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.ui.viewmodels.ExportImportState
import com.batteryexpert.ui.viewmodels.SettingsViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val state by viewModel.state.collectAsState()
    val rawPacketLog by viewModel.rawPacketLog.collectAsState()

    val provider by viewModel.provider.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    var apiKeyInput by remember { mutableStateOf(apiKey) }

    LaunchedEffect(provider, apiKey) {
        apiKeyInput = apiKey
    }

    var statusFeedback by remember { mutableStateOf<String?>(null) }
    var isSuccessFeedback by remember { mutableStateOf(true) }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            viewModel.startScan()
        }
    }

    fun checkAndStartScan() {
        val ok = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (ok) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val currentState = state
            if (currentState is ExportImportState.ExportSuccess) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(currentState.json.toByteArray())
                    }
                    statusFeedback = "Export erfolgreich in Datei gespeichert!"
                    isSuccessFeedback = true
                    viewModel.resetState()
                } catch (e: Exception) {
                    statusFeedback = "Fehler beim Schreiben der Datei: ${e.localizedMessage}"
                    isSuccessFeedback = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    viewModel.importData(jsonString)
                }
            } catch (e: Exception) {
                statusFeedback = "Fehler beim Lesen der Datei: ${e.localizedMessage}"
                isSuccessFeedback = false
            }
        }
    }

    LaunchedEffect(state) {
        val currentState = state
        when (currentState) {
            is ExportImportState.ExportSuccess -> {
                exportLauncher.launch("battery_expert_export.json")
            }
            is ExportImportState.ImportSuccess -> {
                statusFeedback = "Erfolgreich ${currentState.count} Akku(s) inkl. Profilen importiert!"
                isSuccessFeedback = true
                viewModel.resetState()
            }
            is ExportImportState.Error -> {
                statusFeedback = currentState.message
                isSuccessFeedback = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen & Debug") }
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
            // 1. Bluetooth Connection Card (TOP)
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
                    Text("Bluetooth-Verbindung (MC5000)", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "Status: Verbunden"
                            ConnectionState.CONNECTING -> "Status: Verbinde…"
                            ConnectionState.DISCONNECTING -> "Status: Trenne…"
                            else -> "Status: Getrennt"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> Color(0xFF43A047)
                            ConnectionState.CONNECTING -> Color(0xFFFB8C00)
                            else -> Color(0xFF757575)
                        }
                    )

                    errorMessage?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        OutlinedButton(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Trennen")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.sendStartStop(3) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Alle starten")
                            }

                            Button(
                                onClick = { viewModel.sendStartStop(0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Alle stoppen")
                            }
                        }
                    } else {
                        Button(
                            onClick = { checkAndStartScan() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Geräte suchen")
                        }
                    }

                    if (connectionState == ConnectionState.DISCONNECTED && scannedDevices.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Gefundene Geräte:", style = MaterialTheme.typography.labelLarge)
                        scannedDevices.sortedByDescending { it.isMc5000 }.forEach { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(onClick = { viewModel.connect(device) }) {
                                    Text("Verbinden")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Export/Import Card
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
                        text = "Daten-Sicherung (SAF JSON)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()

                    Text(
                        text = "Exportiere oder importiere Akkus, Lade-Profile und Messdaten als JSON-Datei über das Android Storage Access Framework (SAF).",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportData() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportieren")
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importieren")
                        }
                    }
                }
            }

            statusFeedback?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccessFeedback) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = if (isSuccessFeedback) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 3. Protocol Debug Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Protokoll-Debug (BLE Hex)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    HorizontalDivider()

                    Text(
                        text = "Roh-Bytes der letzten TX/RX BLE-Pakete (z.B. zur Validierung von 0x94 / Status-Paketen):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (rawPacketLog.isEmpty()) {
                                Text(
                                    text = "Keine BLE-Pakete protokolliert",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            } else {
                                rawPacketLog.forEach { logEntry ->
                                    Text(
                                        text = logEntry,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (logEntry.startsWith("TX")) Color(0xFF1E88E5) else Color(0xFF43A047)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. KI-Provider & API-Key Card (BOTTOM)
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
                    Text("KI-Provider & API-Key", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = provider == Provider.GEMINI,
                            onClick = { viewModel.setProvider(Provider.GEMINI) }
                        )
                        Text("Gemini")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = provider == Provider.DEEPSEEK,
                            onClick = { viewModel.setProvider(Provider.DEEPSEEK) }
                        )
                        Text("DeepSeek")
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(if (provider == Provider.GEMINI) "Gemini API-Key" else "DeepSeek API-Key") },
                        placeholder = { Text(if (provider == Provider.GEMINI) "z.B. AIza..." else "z.B. sk-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (provider == Provider.GEMINI) "Key erhalten: https://aistudio.google.com/apikey"
                               else "Key erhalten: https://platform.deepseek.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            viewModel.setApiKey(apiKeyInput)
                            statusFeedback = "API-Key für ${if (provider == Provider.GEMINI) "Gemini" else "DeepSeek"} erfolgreich gespeichert!"
                            isSuccessFeedback = true
                        }
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
