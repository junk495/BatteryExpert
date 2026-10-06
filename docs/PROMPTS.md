# BatteryExpert – Prompts für Android Studio KI

Kopiere die Prompts der Reihe nach in die KI von Android Studio. Die KI hat den
Projektkontext automatisch; bei Bedarf vorherige Dateien erwähnen.

> Reihenfolge wichtig: **Prompt 1 (Codec + Tests) ist das Fundament** – erst wenn die
> Unit-Tests grün sind, mit BLE/UI weitermachen.

---

## Prompt 0 – Projekt-Setup

```
Erstelle ein neues Android-Projekt in Kotlin mit Jetpack Compose (Material 3).
Package: com.batteryexpert. Name: "BatteryExpert". minSdk 26, target/compile 34.

Füge in build.gradle.kts (Version Catalog libs.versions.toml) folgende Abhängigkeiten hinzu:
- androidx.compose BOM, material3, ui, lifecycle-viewmodel-compose, activity-compose
- androidx.navigation:navigation-compose
- androidx.room:room-runtime, room-ktx, ksp room-compiler
- kotlinx-coroutines-android
- com.google.ai.client.generativeai (aktuelle Version)
- Vico für Diagramme (com.patrykpatrick.vico:compose-m3)
Konfiguriere KSP. Erstelle MainActivity mit Scaffold + NavHost und leeren Platzhalter-Screens
(BatteryList, Monitor, Settings). Die App muss fehlerfrei bauen.
```

---

## Prompt 1 – BLE-Protokoll-Codec (Kernstück)

```
Implementiere in com.batteryexpert.data.ble eine reine Protokollschicht (ohne Bluetooth-IO,
nur Byte-Operationen) für den SkyRC MC5000:

Klasse ProtocolCodec mit:
- fun buildPacket(command: Int, data: ByteArray): ByteArray
  Format: [0x0F, length, command, ...data, checksum]
  length = Anzahl Bytes nach 0x0F (command + data + checksum).
  checksum = Summe aller Bytes von command bis Ende data, mod 256.
- fun checksumValid(packet: ByteArray): Boolean
- fun buildStatusRequest(slotBitmask: Int): ByteArray   // command 0x91
- fun buildStartStop(action: Int): ByteArray            // command 0x93 (0=stop all,1=slot1,2=slot2,3=start all,4=slot3,8=slot4)
- fun parseStatus(packet: ByteArray): SlotStatus        // command 0x91 Antwort

data class SlotStatus(
  slot: Int, voltageV: Float, currentA: Float, temperatureC: Float,
  capacityMah: Int, elapsedSeconds: Long, internalResistanceMOhm: Int,
  status: String, mode: String, error: String, chemistry: String
)

Status-Antwort-Offsets (big-endian, packet[0]=0x0F, packet[2]=command):
  [4:6] Strom/1000, [6:8] Spannung/1000, [8:10] Temp/1000 (Werte <1 => 0),
  [10:12] Kapazität (mAh), [12:16] Zeit (Sekunden), [16:18] Innenwiderstand,
  [18] Status, [19] Modus, [20] Fehler, [21] Chemie.

Chemie-Mapping: 0=Li-Ion,1=Li-Ion HV,2=LiFePO4,3=NiMH,4=NiCd,5=Eneloop,6=NiZn,7=RAM,8=LTO,9=Na-Ion.
Status-Mapping: 0=Standby,1=Processing,2=Charging,3=Discharging,4=Resting,5=Completed,6=Completed.
Fehler: 1..13 als Text ("Input voltage too low", "Input voltage too high", "Connection break",
"Capacity limit reached", "Time limit reached", "Internal temperature too high", "Calibration failed",
"High internal resistance", "Connection break", "Battery type error", "Overload protection",
"Reversed polarity", "Fully charged").

Wichtig: Notifications können auf 20 Bytes gekürzt sein (BLE-MTU) – parseStatus muss
auch mit kurzen Paketen robust umgehen (fehlende Bytes defensiv behandeln).
Schreibe zusätzlich Unit-Tests (JUnit) für buildPacket, checksumValid und parseStatus
(inkl. Kürzungs-Fall). Die Tests müssen grün sein.
```

---

## Prompt 2 – BLE-Manager (Scan/Connect/Notify)

```
Implementiere in com.batteryexpert.data.ble einen Mc5000BleManager (Kotlin, coroutines/Flow,
native android.bluetooth.le):

- fun scanDevices(): Flow<List<BleDevice>>  (BluetoothLeScanner, filtert Namen die "MC5000" enthalten)
- suspend fun connect(device: BleDevice): Result<Unit>
  (BluetoothGatt, Service 0000ffe0-0000-1000-8000-00805f9b34fb,
   Char 0000ffe1-0000-1000-8000-00805f9b34fb; requestMtu(247) versuchen;
   CharDescriptor CLIENT_CHARACTERISTIC_CONFIG aktivieren für NOTIFY)
- fun slotStatus(slotBitmask: Int): Flow<SlotStatus>
  schreibt buildStatusRequest und emittiert parseStatus der eingehenden Notifications
- suspend fun writePacket(packet: ByteArray)
- suspend fun disconnect()

Nutze ProtocolCodec aus Prompt 1. Erforderliche Permissions: BLUETOOTH_SCAN, BLUETOOTH_CONNECT
(+ BLUETOOTH_ADVERTISE nicht nötig), Laufzeit-Anfrage. Behandle Verbindungsstatus als StateFlow.
Keine Fremd-BLE-Library verwenden.
```

---

## Prompt 3 – Room-Datenbank + DAOs

```
Erstelle in com.batteryexpert.data.db Room-Entities + DAOs + AppDatabase (Version 1):

BatteryEntity(id, brand, model, chemistry: String, nominalVoltageV: Float, capacityMah: Int,
size: String, internalResistanceMOhm: Int, maxChargeCurrentMa: Int, maxDischargeCurrentMa: Int,
purchaseDate: Long?, notes: String?, createdAt: Long, updatedAt: Long)

ChargeProfileEntity(id, batteryId: Long, mode: String, chargeCurrentMa: Int,
dischargeCurrentMa: Int, targetVoltageMv: Int, cutoffVoltageMv: Int, terminationCurrentMa: Int,
cycleDirection: Int, cycleCount: Int, restChargeMin: Int, restDischargeMin: Int,
trickleChargeMa: Int, deltaPeakMv: Int, cutoffTimerMin: Int, maxTimeMin: Int)

MeasurementEntity(id, batteryId: Long, slot: Int, timestamp: Long, voltageV: Float,
currentA: Float, temperatureC: Float, capacityMah: Int, internalResistanceMOhm: Int,
phase: String, status: String)

DAOs mit Flow-Queries: getBatteries(), getBatteryById(id), insert/update/delete,
getProfilesForBattery(batteryId), getMeasurementsForBattery(batteryId), insertMeasurements(List).
Foreign Key mit CASCADE von profiles/measurements auf battery.
```

---

## Prompt 4 – Repository + ViewModels

```
Erstelle Repositories (BatteryRepository, BleRepository, MeasurementRepository) und ViewModels:
BatteryListViewModel, BatteryDetailViewModel, MonitorViewModel.

MonitorViewModel pollt per Flow alle 4 Slots (Bitmasken 1,2,4,8) über Mc5000BleManager
und liefert einen Flow<List<SlotStatus>>. Verbindungszustand wird mitgeliefert.
BatteryListViewModel: Suche/Sortierung nach Marke/Modell/Chemie.
BatteryDetailViewModel: lädt Akku + Profile + Messungen.
```

---

## Prompt 5 – UI: Akku-Bibliothek

```
Implementiere die Compose-Screens:
- BatteryListScreen: LazyColumn mit Karten (Marke, Modell, Chemie, Kapazität), SearchBar,
  FloatingActionButton "Neu". Navigation zu Detail/Edit.
- BatteryEditScreen: Formular für alle BatteryEntity-Felder, Chemie als Dropdown,
  Größe als Dropdown (18650, 21700, AA, AAA, C, D, 14500, 16340, 18350, 26650, 32700, Other),
  Speichern/Cancel.
- BatteryDetailScreen: zeigt Stammdaten, Liste der Lade-Profile, Messungs-Verlauf (Vico-Chart),
  Buttons "Profil senden" und "Löschen".
Nutze die ViewModels aus Prompt 4. Material 3, deutsche UI-Texte.
```

---

## Prompt 6 – UI: Live-Monitoring

```
Implementiere MonitorScreen (Compose):
- Scan-Button, Geräteliste, Verbinden/Trennen.
- 4 Slot-Karten (Slot 1-4) mit Live-Werten: Spannung, Strom, Temperatur, Kapazität, Zeit, IR, Status, Modus, Chemie, Fehler.
- Farbkodierung: Charging=rot, Discharging=blau, Completed=grün, Standby=grau.
- Optional: Vico-Liniendiagramm für Spannung über Zeit (Ringpuffer der letzten N Werte pro Slot).
Nutze MonitorViewModel. Zeige Permission-Abfrage korrekt an.
```

---

## Prompt 7 – UI: Konfig-Editor + Senden an MC5000

> ⚠️ **Ersetzt:** Der manuelle Konfig-Editor wurde durch das **ConfigSheet im Monitor**
> ersetzt (Slot antippen → Zell-Setting wählen + nachjustieren → Slots auswählen → senden).
> Dieser Prompt ist nur noch historisch.

```
Implementiere ConfigEditorScreen:
Wählt eine BatteryEntity + ein ChargeProfileEntity, zeigt alle Felder (Modus als Dropdown:
Charge/Storage/Discharge/Cycle/Refresh/Break-in; Chemie aus Battery), Slot-Auswahl (1-4),
und einen Button "An MC5000 senden".

Beim Senden: baue das 0x94-Konfig-Paket. Das exakte Byte-Layout für 0x94 bitte aus der
Referenz-Doku übernehmen: https://raw.githubusercontent.com/rssdev10/skyrc-mc-rs/main/docs/PROTOCOL.md
(Abschnitt 0x94 / build_charge_config_command) und in ProtocolCodec als
buildChargeConfig(profile: ChargeProfileEntity, chemistry: Int): ByteArray implementieren.
Danach sende 0x93 mit Start-Aktion für den Slot.

Zeige den ACK der Konfig an (Antwort 0f 04 94 <slot> 01 <cks> = OK) und behandle Fehler.
Wichtig: Wertebereichs-Prüfung (z. B. Strom 50-5000 mA, Spannung je Chemie) vor dem Senden.
```

---

## Prompt 8 – KI-Recherche (Gemini)

```
Implementiere AiResearchScreen + AiRepository:
- Textfeld für Akku-Modell/Suchbegriff, Button "Recherchieren".
- Nutzt die Gemini API (com.google.ai.client.generativeai) mit responseSchema für ein JSON-Objekt,
  das exakt den BatteryEntity-Feldern entspricht (brand, model, chemistry, nominalVoltageV,
  capacityMah, size, internalResistanceMOhm, maxChargeCurrentMa, maxDischargeCurrentMa, notes).
- System-Prompt: "Du bist Akku-Experte. Recherchiere technische Daten des Akkus. Antworte NUR
  als JSON gemäß Schema. Unbekannte Werte => null. Gib typische Lade-/Entladeschlusswerte an."
- Das Ergebnis wird in ein prüfbares Formular geladen (BatteryEditScreen wiederverwenden),
  Nutzer korrigiert und speichert.
- API-Key in local.properties/BuildConfig auslagern (nicht hartcodieren).
```

---

## Prompt 9 – Import/Export + Feinschliff

```
Implementiere JSON-Export/Import für Batterien (inkl. Profile + Messungen) über SAF
(Storage Access Framework, ACTION_CREATE_DOCUMENT / ACTION_OPEN_DOCUMENT) mit Gson/kotlinx-serialization.
Füge in Settings einen Export/Import-Bereich hinzu. Ergänze die Protokoll-Debug-Ansicht
(Roh-Bytes der letzten Pakete anzeigen, hilft beim Validieren des 0x94-Layouts).
Räume auf, stelle sicher dass die App ohne Warnungen baut.
```

---

## Prompt 10 – Datenbank-Migration (Zelltyp + Zelle + Testergebnis)

```
Refaktoriere die Room-Datenbank auf ein zweistufiges Modell (Version 2).
Grundlage: docs/KONZEPT.md Abschnitt 4.

1. Neue Entity CellTypeEntity (Seriendaten des Zelltyps) mit diesen Feldern:
   id: Long, manufacturer: String, model: String, aliases: String?, size: String, chemistry: String,
   nominalVoltageV: Float, nominalCapacityMah: Int,
   nominalEnergyWh: Float?, typicalInternalResistanceMOhm: Int?, irMeasurementNote: String?,
   chargeEndVoltageV: Float?, chargeCurrentStandardMa: Int?, chargeCurrentOptimalMa: Int?,
   chargeCurrentMaxMa: Int?, chargeTerminationCurrentMa: Int?, chargeTempMinC: Int?, chargeTempMaxC: Int?,
   deltaPeakMv: Int?, capacityCutoffMah: Int?, trickleChargeMa: Int?, keepVoltageMv: Int?,   // NiMH-Ladeparameter
   dischargeCutoffRecommendedV: Float?, dischargeCutoffAbsoluteMinV: Float?,
   dischargeCurrentStandardMa: Int?, dischargeCurrentMaxContinuousMa: Int?, dischargeCurrentMaxPulseMa: Int?,
   dischargeTempMinC: Int?, dischargeTempMaxC: Int?,
   storageVoltageV: Float?, storageTempMinC: Int?, storageTempMaxC: Int?, selfDischargePerMonthPercent: Float?,
   cycleLifeTo80Percent: Int?, cycleLifeNote: String?, maxCellTempC: Int?,
   fastChargeCurrentMa: Int, fastDischargeCurrentMa: Int, slowChargeCurrentMa: Int, slowDischargeCurrentMa: Int,
   measuredTypicalCapacityMah: Int?, measuredTypicalIRMOhm: Int?,
   sourceUrl: String?, extras: String?, notes: String, createdAt: Long, updatedAt: Long
   (Nullable = optional; extras ist eine JSON-Spalte für freie Zusatzfelder.)

2. BatteryEntity auf die "konkrete Zelle" reduzieren (Datenblatt-Felder entfallen hier):
   id: Long, cellTypeId: Long (FK auf CellTypeEntity, CASCADE), label: String, serialNumber: String?,
   origin: String?, purchaseDate: Long?, purchaseCapacityMah: Int?, purchaseInternalResistanceMOhm: Int?,
   location: String?, status: String (NEW/USED/SORT_OUT/DEFECT), notes: String?, createdAt: Long, updatedAt: Long

3. TestResultEntity (unverändert):
   id, batteryId (FK), slot, timestamp, testType (IR_ONLY/FAST/SLOW),
   chargeCurrentMa, dischargeCurrentMa, cutoffVoltageMv,
   measuredCapacityMah, internalResistanceMOhm, sohPercent, recommendation, note

4. DAOs: CellTypeDao (insert/update/delete, getAll(): Flow, getById(id)),
   BatteryDao anpassen (getBatteriesForType(cellTypeId)), TestResultDao wie gehabt.

5. AppDatabase: Version 2, alle Entities + DAOs. Da das Modell umgebaut wird, für die
   Entwicklungsphase fallbackToDestructiveMigration() verwenden (Datenverlust als Kommentar kennzeichnen).

6. Aufrufer anpassen: BatteryEditScreen pflegt jetzt CellTypeEntity; BatteryListScreen zeigt
   Zelltypen (statt Einzelzellen); AiRepository liefert ein CellTypeEntity (Schema in Prompt 13);
   Export/Import auf CellTypeEntity + BatteryEntity + TestResultEntity erweitern.

7. Unit-Tests aktualisieren (AppDatabaseTest, ExportImportRepositoryTest, ViewModelsTest), bis grün.
```

---

## Prompt 11 – Bewertungslogik (IR + SOH + Ampel)

```
Implementiere in com.batteryexpert.data.assessment eine reine Kotlin-Logik (ohne Android/UI):

enum Recommendation { OK, WATCH, SORT_OUT } mit deutscher Beschriftung
(OK = "OK", WATCH = "Beobachten", SORT_OUT = "Aussortieren").

object AssessmentLogic:
- fun irRecommendation(measuredIR: Int, targetIR: Int): Recommendation
  Verhältnis = measuredIR / targetIR (bei targetIR <= 0 -> OK).
  < 1.5 -> OK, 1.5..2.0 -> WATCH, > 2.0 -> SORT_OUT.
- fun sohRecommendation(measuredCapacityMah: Int, ratedCapacityMah: Int): Pair<Float, Recommendation>
  soh = measured * 100f / rated (bei rated <= 0 -> 0f und WATCH).
  > 90 -> OK, 80..90 -> WATCH, < 80 -> SORT_OUT.
- fun computeDefaults(ratedCapacityMah: Int): TestCurrents
  TestCurrents(fastChargeMa, fastDischargeMa, slowChargeMa, slowDischargeMa).
  1C = ratedCapacityMah mA. fastCharge = 1C, fastDischarge = min(1C, 2000),
  slowCharge = 0.5C, slowDischarge = 0.2C (auf ganze mA runden).

Schreibe Unit-Tests AssessmentLogicTest für irRecommendation, sohRecommendation und
computeDefaults (inkl. Grenzfälle: targetIR=0, measured=0, rated<=0, Entlade-Limit 2000 mA).
```

---

## Prompt 12 – Test-Tab (Navigation + Screen + ViewModel)

```
Baue einen neuen Tab "Test" in die untere Navigation ein (MainActivity, neben Akkus/Monitor/Einstellungen).

1. Repository com.batteryexpert.data.repository.TestRepository:
   - erhält BatteryDao + CellTypeDao + TestResultDao.
   - fun observeBatteries(): Flow<List<BatteryEntity>>
   - suspend fun saveResult(result: TestResultEntity)
   - fun observeResults(batteryId: Long): Flow<List<TestResultEntity>>
   - suspend fun getCellType(cellTypeId: Long): CellTypeEntity?

2. TestViewModel:
   - Zellen laden (für Slot-Auswahl); zu jeder Zelle den zugehörigen CellTypeEntity auflösen.
   - Pro Slot (1-4): zugeordnete Zelle + Testart (Schnell/Genau) halten.
   - Erstbewertung: IR des Slots aus dem Live-Status lesen und mit
     CellType.typicalInternalResistanceMOhm vergleichen -> AssessmentLogic.irRecommendation.
   - Test starten: für jeden belegten Slot die 0x94-Konfig (Cycle C->D, 1 Zyklus) mit den
     Testströmen des ZELLTYPS bauen und 0x93 starten.
     Ströme: Schnelltest = CellType.fastCharge/fastDischarge, genauer = CellType.slowCharge/slowDischarge.
     Cutoff-Spannung aus der Chemie des Zelltyps.
   - Fortschritt anzeigen, auf "Completed" warten.
   - Ergebnis nur bei vollständigem Abschluss speichern: gemessene Kapazität + IR aus dem
     Slot-Status, sohPercent (gegen CellType.nominalCapacityMah) + Recommendation berechnen,
     als TestResultEntity speichern.

3. Screen ui/test/TestScreen (Compose, deutsche Texte): Zellen-Dropdown (aus DB), Testart-Radio
   (Schnell/Genau), "IR prüfen" (gemessen vs. Soll + farbige Empfehlung), Statusanzeige,
   Buttons "Tests starten"/"Abbrechen", darunter SOH-Verlauf der Zelle.

Nutze AssessmentLogic aus Prompt 11 und das Datenmodell aus Prompt 10.
```

---

## Prompt 13 – Zelltypen befüllen (KI-Schema + Zellen anlegen)

```
Passe das Befüllen an das zweistufige Modell an (docs/KONZEPT.md Abschnitt 4):

1. Tab "Akkus" zeigt jetzt ZELLTYPEN (CellTypeEntity), nicht Einzelzellen.
   - Liste der Typen (Hersteller + Modell + Chemie + Kapazität).
   - Detail-Screen: alle Felder des Typs (gruppiert; optionale Felder bleiben leer).

2. KI-Recherche (AiRepository / Prompt 8) auf CellTypeEntity umstellen:
   - Gemini-Systemprompt liefert JSON mit den CellTypeEntity-Feldern (inkl. der optionalen
     Spannungen/Ströme/Temperaturen, unbekannt => null).
   - Ergebnis in das Typ-Formular laden (prüfbar), Nutzer korrigiert, speichert.

3. Testströme automatisch vorbelegen: beim Anlegen eines Typs
   AssessmentLogic.computeDefaults(nominalCapacityMah) aufrufen und in die vier
   fast/slow-Felder übernehmen (überschreibbar im Formular).

4. "Zellen anlegen" aus einem Typ: Button "N Zellen anlegen" mit Anzahl-Feld ->
   erzeugt N BatteryEntity mit cellTypeId + Auto-Label ("<Modell> #1", "#2", ...).

5. Export/Import: JSON um cellTypes erweitern (versioniert). Optional CSV-Export der TestResult.
```

---

## Hinweise zur Nutzung

- **Vor Phase 4 (Schreiben):** exaktes `0x94`-Layout aus der Referenz-Doku ziehen
  (Prompt 7 verweist darauf). Der Debug-Screen (Prompt 9) hilft beim Vergleich mit dem echten Gerät.
- **Referenz-Doku:** `https://raw.githubusercontent.com/rssdev10/skyrc-mc-rs/main/docs/PROTOCOL.md`
- **Abkürzung:** Der Rust-Stack `skyrc-mc-rs` kann parallel als Referenz-/Debug-Werkzeug
  genutzt werden, um Pakete zu vergleichen.

