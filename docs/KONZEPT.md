# BatteryExpert – Konzept

Handy-App (Android) zum Verwalten von Akku-Daten und zum direkten Ansteuern des
SkyRC MC5000 (4-Slot-Ladegerät) per Bluetooth Low Energy (BLE).

> iOS wird **nicht** unterstützt. Entwicklung ausschließlich in Android Studio.

---

## 1. Ziel

1. Akku-Daten (Spezifikationen, Lade-/Entladeparameter, Messreihen) lokal ablegen und verwalten.
2. Akku-Daten mit KI recherchieren und strukturiert in die App übernehmen.
3. Lade-/Entladeparameter direkt aus der App an den MC5000 übertragen.
4. Live-Daten des MC5000 (Spannung, Strom, Kapazität, Innenwiderstand) anzeigen.
5. **Zellen standardisiert bewerten** (Innenwiderstand + Kapazität/SOH) → Alterung verifizieren.

---

## 2. Tech-Stack

| Baustein    | Wahl                                                     | Begründung                                                          |
|-------------|----------------------------------------------------------|---------------------------------------------------------------------|
| Sprache     | Kotlin                                                   | Standard                                                            |
| UI          | Jetpack Compose + Material 3                             | modern, KI-gut unterstützt                                          |
| Architektur | MVVM + Repository + Flow                                 | saubere Trennung                                                    |
| Datenbank   | Room (SQLite)                                            | lokale Akku-Datenbank                                               |
| BLE         | Nativ `android.bluetooth.le` (BluetoothLeScanner/Gatt)   | volle Kontrolle über MTU + Notifications, keine Fremdabhängigkeit   |
| KI (in App)| Gemini API (`com.google.ai.client.generativeai`)         | JSON-Response-Schema, passt zur Android-Studio-KI                   |
| Navigation  | Navigation-Compose                                       | Standard                                                            |
| DI          | Hilt (optional, nachrüstbar)                             | –                                                                   |
| Diagramme   | Vico (`com.patrykpatrick.vico:compose-m3`)               | Live-Graphen                                                        |

---

## 3. Module / Struktur

```
com.batteryexpert/
  BatteryExpertApp.kt  (Application, Singleton-BLE-Manager)
  MainActivity.kt
  data/
    ble/        Mc5000BleManager, Mc5000Service (Foreground), ProtocolCodec,
                SlotStatus, SlotHistory, BleDevice
    db/         Room: Entities (CellType, Battery, TestResult, ChargeProfile, Measurement), DAOs, AppDatabase
    assessment/ AssessmentLogic (IR/SOH/Empfehlung)
    repository/ Battery-, Ble-, Ai-, ExportImport-, Measurement-, Test-Repository
    ApiKeyStore.kt (Gemini-/DeepSeek-Key)
  ui/
    screens/    BatteryListScreen, BatteryDetailScreen, BatteryEditScreen,
                MonitorScreen (mit ConfigSheet), AiResearchScreen, SettingsScreen
    test/       TestScreen (Zell-Bewertung)
    viewmodels/ ...
    theme/
```

---

## 4. Datenmodell (Room)

Zweistufig: **Zelltyp (Seriendaten)** ← **Einzelzelle** ← **Testergebnis**.
(`?` = optional/nullable)

### 4.1 Entities

1. **CellTypeEntity** (Seriendaten des Zelltyps, Tab „Akkus")
   `id, manufacturer, model, aliases?, size, chemistry (enum),`
   `nominalVoltageV, nominalCapacityMah, nominalEnergyWh?, typicalInternalResistanceMOhm?, irMeasurementNote?,`
   `chargeEndVoltageV?, chargeCurrentStandardMa?, chargeCurrentOptimalMa?, chargeCurrentMaxMa?, chargeTerminationCurrentMa?, chargeTempMinC?, chargeTempMaxC?,`
   `deltaPeakMv?, capacityCutoffMah?, trickleChargeMa?, keepVoltageMv?,  (NiMH-Ladeparameter)`
   `dischargeCutoffRecommendedV?, dischargeCutoffAbsoluteMinV?, dischargeCurrentStandardMa?, dischargeCurrentMaxContinuousMa?, dischargeCurrentMaxPulseMa?, dischargeTempMinC?, dischargeTempMaxC?,`
   `storageVoltageV?, storageTempMinC?, storageTempMaxC?, selfDischargePerMonthPercent?,`
   `cycleLifeTo80Percent?, cycleLifeNote?, maxCellTempC?,`
   `fastChargeCurrentMa, fastDischargeCurrentMa, slowChargeCurrentMa, slowDischargeCurrentMa,`
   `measuredTypicalCapacityMah?, measuredTypicalIRMOhm?,`
   `sourceUrl?, extras?, notes, createdAt, updatedAt`

2. **BatteryEntity** (konkrete Zelle, referenziert den Typ)
   `id, cellTypeId (FK → CellTypeEntity), label, serialNumber?, origin?,`
   `purchaseDate?, purchaseCapacityMah?, purchaseInternalResistanceMOhm?, location?,`
   `status (NEW/USED/SORT_OUT/DEFECT), notes?, createdAt, updatedAt`
   → `origin` = Herkunft der Zelle (Kommentar: „aus altem Laptop-Akku", „eBay" …),
   `notes` = freies Kommentarfeld, `location` = Lagerort/Box.

3. **TestResultEntity** (Ergebnisse der Zell-Bewertung)
   `id, batteryId (FK), slot, timestamp, testType (IR_ONLY/FAST/SLOW),`
   `chargeCurrentMa, dischargeCurrentMa, cutoffVoltageMv,`
   `measuredCapacityMah, internalResistanceMOhm, sohPercent,`
   `recommendation (OK/WATCH/SORT_OUT), note`

4. **ChargeProfileEntity** (spiegelt die `0x94`-Felder wider)
   `id, batteryId (FK), mode, chargeCurrentMa, dischargeCurrentMa, targetVoltageMv,`
   `cutoffVoltageMv, terminationCurrentMa, cycleDirection, cycleCount, restChargeMin,`
   `restDischargeMin, trickleChargeMa, deltaPeakMv, cutoffTimerMin, maxTimeMin`

5. **MeasurementEntity**
   `id, batteryId, slot, timestamp, voltageV, currentA, capacityMah,`
   `internalResistanceMOhm, phase (charge/discharge/cycle), status`

### 4.2 Beziehungen
- `BatteryEntity.cellTypeId → CellTypeEntity.id` (1 Zelle : 1 Typ, 1 Typ : n Zellen).
- `TestResultEntity.batteryId → BatteryEntity.id`, ebenso `ChargeProfileEntity`/`MeasurementEntity`.

### 4.3 Speicher & Export
- **Speicher:** Room/SQLite, echte Spalten (stabil, typisiert, filterbar).
  `extras` (JSON) nur als Sammelbecken für freie Zusatzfelder am Zelltyp.
- **Export:** versioniertes JSON (Backup/Portabilität) über die Einstellungen, enthält
  `cellTypes`, `batteries`, `testResults`, `chargeProfiles`, `measurements`.
  Optional zusätzlich CSV-Export der `testResults` (für Excel).

---

## 5. BLE-Protokoll (reverse-engineered)

Referenz-Implementierung: `rssdev10/skyrc-mc-rs` (Rust, MIT)
– `docs/PROTOCOL.md` und `mc5000-protocol/src/bluetooth.rs`.

- **Service UUID:** `0000ffe0-0000-1000-8000-00805f9b34fb`
- **Characteristic UUID:** `0000ffe1-0000-1000-8000-00805f9b34fb`
  (READ | WRITE_WITHOUT_RESPONSE | NOTIFY)
- **Paketformat:** `0x0F | Länge | Kommando | Daten… | Prüfsumme`
  - Länge = Anzahl Bytes nach `0x0F` (Kommando + Daten + Prüfsumme)
  - Prüfsumme = Summe aller Bytes von Kommando bis Ende der Daten, mod 256

### Kommandos

| Kommando | Bedeutung                                     |
|----------|-----------------------------------------------|
| `0x91`   | Slot-Status lesen                             |
| `0x94`   | Lade-/Entlade-Konfiguration schreiben         |
| `0x93`   | Start / Stopp                                 |
| `0x25`   | evtl. „Session-Unlock"/Handshake (noch offen) |
| `0xEA`   | Telemetrie-Blob (Zweck unbekannt)             |
| `0x02`   | Keep-alive / Ack                              |

### Status lesen (`0x91`)

- Request: `0F 03 91 <slotBitmask> <cks>` – Slot-Bitmasken: `1, 2, 4, 8` (Slot 1–4)
- Antwort-Offsets (big-endian, `packet[0]=0x0F`, `packet[2]=0x91`):

| Bytes     | Bedeutung       | Einheit      |
|-----------|-----------------|--------------|
| `[4:6]`   | Strom           | /1000 → A    |
| `[6:8]`   | Spannung        | /1000 → V    |
| `[8:10]`  | Temperatur      | /1000 → °C   |
| `[10:12]` | Kapazität       | mAh          |
| `[12:16]` | Zeit            | Sekunden     |
| `[16:18]` | Innenwiderstand | mΩ           |
| `[18]`    | Status          | –            |
| `[19]`    | Modus           | –            |
| `[20]`    | Fehler          | –            |
| `[21]`    | Chemie          | –            |

### Chemie-Mapping

`0=Li-Ion, 1=Li-Ion HV, 2=LiFePO4, 3=NiMH, 4=NiCd, 5=Eneloop, 6=NiZn, 7=RAM, 8=LTO, 9=Na-Ion`

### Ziel-/Cutoff-Spannungen je Chemie

| Chemie    | Zielspannung | Cutoff-Spannung |
|-----------|--------------|-----------------|
| Li-Ion    | 4.20 V       | 3.20 V          |
| Li-Ion HV | 4.35 V       | 3.40 V          |
| LiFePO4   | 3.65 V       | 2.90 V          |
| NiMH      | 1.65 V       | 0.90 V          |
| NiCd      | 1.65 V       | 0.90 V          |
| Eneloop   | 1.65 V       | 0.90 V          |
| NiZn      | 1.90 V       | 1.10 V          |
| RAM       | 1.65 V       | 0.90 V          |
| LTO       | 2.85 V       | 1.80 V          |
| Na-Ion    | 4.00 V       | 2.00 V          |

### Modi (`0x94` Byte, bzw. Status-Modusbyte)

`0x00 Charge, 0x01 Storage, 0x02 Discharge, 0x03 Cycle, 0x04 Refresh, 0x05 Break-In`

> Hinweis: „Break-In" wird von der offiziellen App intern als Discharge (0x02) mit
> speziellen Parametern umgesetzt (bestätigt durch Live-Tests in `skyrc-mc-rs`).

### Status-Mapping (`0x91` data[18])

`0=Standby, 1=Processing, 2=Charging, 3=Discharging, 4=Resting, 5=Completed, 6=Completed`

### Konfig schreiben (`0x94` + Start `0x93`)

- Start Slot 1: `0F 03 93 01 <cks>` / Stop alle: `0F 03 93 00 <cks>` / Start alle: `0F 03 93 03 <cks>`
- Konfig-ACK: Antwort `0f 04 94 <slot> 01 <cks>` = OK.
- Konfig-Paket (`0x94`) hat ~33 Datenbytes. Bekannte Offsets:
  - `data[32]` = Chemie
  - `data[21]` = Zyklus-Richtung (0=C→D, 1=D→C, 2=C→D→C, 3=D→C→D)
  - `data[20]` = Zykluszahl
  - `data[16–17]` = Ladepause (BE, min), `data[18–19]` = Entladepause (BE, min)
  - `data[22]` = Delta-Peak (mV), `data[23]` = Trickle-Strom (÷10), `data[24–25]` = Keep-Spannung (BE)
  - `data[27–28]` = Cutoff-Timer (BE, min), max. Betriebszeit danach

---

### Verbindung dauerhaft halten

- **Foreground-Service** `Mc5000Service` (Typ `connectedDevice`) mit `PARTIAL_WAKE_LOCK`
  hält die BLE-Verbindung auch im Standby/Doze.
- **Auto-Reconnect:** letztes Gerät (MAC) wird persistiert; nach Abbruch/App-Start wird
  automatisch neu verbunden (Backoff bis 30 s). Manuelles Trennen deaktiviert den Auto-Reconnect.
- **Polling + Historie** laufen im Singleton-`Mc5000BleManager` (Application-Scope), nicht in der UI.

---

## 6. Wichtige Risiken / offene Punkte

1. **Exaktes `0x94`-Byte-Layout** muss gegen die Referenz-Doku
   (`rssdev10/skyrc-mc-rs` → `docs/PROTOCOL.md`) bzw. das echte Gerät verifiziert werden.
2. **`0x25`-Handshake**: Eventuell nötig, um Schreib-Kommandos zu „entriegeln". Ohne ihn
   kann es sein, dass das Gerät Konfigs zwar bestätigt (ACK), aber nicht anwendet.
3. **BLE-Notification-Kürzung**: Antworten sind 23 Bytes, Notifications können auf 20 Bytes
   (Default-MTU) gekürzt werden. Parser müssen robust mit kurzen Paketen umgehen.
4. iOS wird nicht unterstützt (nur Android).

→ Deshalb **Phase 1 = Protokoll-Spike mit Debug-Screen**, um Lesen/Schreiben gegen
das echte Gerät zu validieren.

---

## 7. KI-Recherche (Idealvorstellung)

- Nutzer gibt Akku-Typ/Modell ein (z. B. „Samsung INR18650-30Q").
- Gemini (optional mit Grounding/Websuche) liefert strukturiertes JSON nach `BatteryEntity`-Schema.
- App zeigt Ergebnis als prüfbares Formular → Nutzer korrigiert → speichert.
- Alternativ: Import extern recherchierter JSON/CSV.

---

## 8. Test-Tab (Bewertung & Zellalterung)

Standardisierte **Alterungsbewertung** je Zelle (IR + Kapazität/SOH).
Messverfahren + Schwellen: [`docs/TESTPROTOKOLL.md`](TESTPROTOKOLL.md).

### Workflow (pro Slot)
1. **Zelle zuordnen** – pro Slot 1–4 eine Zelle aus der Datenbank wählen (Pflicht).
2. **Erstbewertung (IR)** – kurzer Messvorgang → IR lesen → „gemessen vs. Soll" → Empfehlung
   (OK / beobachten / aussortieren).
3. **Testart wählen** – Schnelltest (hohe Ströme) oder genauer Test (niedrige Ströme),
   Ströme aus `CellTypeEntity`.
4. **Durchführen** – Cycle C→D starten, Fortschritt anzeigen, auf „Completed" warten.
5. **Ergebnis** – SOH % + IR + Ampel, speichern als `TestResultEntity`.

### Regeln
- **Nur Datenbank-Zellen testbar** (Referenz + Zuordnung nötig).
- **Ergebnis nur bei vollständigem Abschluss speichern**, sonst verwerfen.
- Testströme automatisch aus `nominalCapacityMah` des Zelltyps vorbelegen, pro Typ überschreibbar.

### Screens
`ui/test/`: `TestScreen` (Slot-Zuordnung + Erstbewertung + Test), `TestResultScreen` (Verlauf),
ViewModel `TestViewModel`, Repository `TestRepository`.

---

## 9. Phasen

0. Projekt-Setup
1. **Protokoll-Spike + Debug-Screen** (Fundament, Unit-Tests)
2. Datenbank + CRUD + Import/Export
3. BLE nur-Lesen (Monitoring)
4. BLE Konfig schreiben
5. KI-Recherche
6. **Test-Tab (Zell-Bewertung)** – Erstbewertung (IR) + Schnell-/Genau-Test + SOH
7. Feinschliff & Packaging

---

## 10. Referenzen

- `https://github.com/rssdev10/skyrc-mc-rs` – Rust, komplette MC5000-BLE-Implementierung (Schreiben + Lesen)
- `https://github.com/kolinger/skyrc-mc3000` – Python, MC5000-BLE-Monitor (nur Lesen), MC3000-USB-Profiles
- `https://www.skyrc.com/MC5000` – offizielle Produktseite

