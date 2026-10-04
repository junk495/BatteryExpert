# BatteryExpert – Agenten-Kontext

Android-App (nur Android) zur Verwaltung von Akku-Daten und zur Steuerung des
SkyRC MC5000 per Bluetooth Low Energy (BLE).

## Zweck
1. Akku-Daten (Spezifikationen, Lade-/Entladeparameter, Messreihen) lokal verwalten.
2. Akku-Daten per Gemini recherchieren und strukturiert übernehmen.
3. Lade-/Entladeparameter direkt an den MC5000 senden.
4. Live-Daten der 4 Slots anzeigen.

## Stack
Kotlin · Compose (Material 3) · MVVM + Repository + Flow · Room + KSP · Navigation-Compose ·
Vico · Gson · natives BLE (`android.bluetooth.le`) · Gemini API. `minSdk 26`, `targetSdk 34`.

## Wichtige Dateien
- `data/ble/ProtocolCodec.kt` – Paket-Framing, Prüfsumme, Kommandos (`0x91`/`0x93`/`0x94`), Status-Parser.
- `data/ble/Mc5000BleManager.kt` – Scan/Connect/Notify, MTU, Write (`WRITE_TYPE_NO_RESPONSE`), Timeout.
- `data/db/` – Room-Entities (Battery, ChargeProfile, Measurement) + DAOs.
- `data/repository/` – Battery-, Ble-, Ai-, ExportImport-, Measurement-Repository.
- `ui/screens/` + `ui/viewmodels/` – Compose-UI und ViewModels.
- `MainActivity.kt` – Navigation + manuelle DI.

## Datenmodell
- **BatteryEntity:** id, brand, model, chemistry, nominalVoltageV, capacityMah, size,
  internalResistanceMOhm, maxChargeCurrentMa, maxDischargeCurrentMa, purchaseDate, notes, …
- **ChargeProfileEntity:** batteryId, mode, chargeCurrentMa, dischargeCurrentMa, targetVoltageMv,
  cutoffVoltageMv, terminationCurrentMa, cycleDirection, cycleCount, restChargeMin,
  restDischargeMin, trickleChargeMa, deltaPeakMv, cutoffTimerMin, maxTimeMin
- **MeasurementEntity:** batteryId, slot, timestamp, voltageV, currentA, temperatureC,
  capacityMah, internalResistanceMOhm, phase, status

## BLE-Protokoll (Kurzfassung)
- Service `0000ffe0-0000-1000-8000-00805f9b34fb`, Char `0000ffe1-…`
  (READ | WRITE_WITHOUT_RESPONSE | NOTIFY)
- Paket: `0x0F | Länge | Kommando | Daten… | Prüfsumme` (Summe mod 256 ab Kommandobyte)
- `0x91` Status lesen · `0x94` Konfig schreiben · `0x93` Start/Stopp · `0x25` (Handshake?, offen) · `0x02` Keep-alive
- Status-Offsets (big-endian): `[4:6]` Strom, `[6:8]` Spannung, `[8:10]` Temperatur,
  `[10:12]` Kapazität, `[12:16]` Zeit, `[16:18]` Innenwiderstand, `[18]` Status,
  `[19]` Modus, `[20]` Fehler, `[21]` Chemie (V/A/°C je /1000)
- Slot-Bitmasken `1/2/4/8` → Slots 1–4
- Chemie: `0` Li-Ion, `1` Li-Ion HV, `2` LiFePO4, `3` NiMH, `4` NiCd, `5` Eneloop,
  `6` NiZn, `7` RAM, `8` LTO, `9` Na-Ion

## Offene Punkte / Risiken
- Exaktes `0x94`-Konfig-Layout gegen `docs/KONZEPT.md` und die Referenz
  (`rssdev10/skyrc-mc-rs` → `docs/PROTOCOL.md`) verifizieren.
- Möglicher `0x25`-Session-Unlock für Schreib-Kommandos noch ungeklärt.
- BLE-Notifications können auf 20 Bytes gekürzt sein (MTU).

## Build & Test (Windows)
```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

## Arbeitsteilung & Git (verbindlich)
- **Code schreiben:** ausschließlich in Android Studio (du + Gemini). Cline/VS Code liest,
  analysiert, recherchiert, dokumentiert und liefert exakte Patch-Vorschläge als Text.
- **Validierung:** Cline darf Build/Test/Lint von der CLI ausführen
  (`gradlew assembleDebug`, `gradlew testDebugUnitTest`, `gradlew lint`).
- **Git:** Sämtliche Git-Aktivitäten macht der Nutzer **manuell selbst** – kein Agent führt
  Git-Befehle aus.
- Dieselbe Datei nie gleichzeitig in VS Code und Android Studio bearbeiten.

## Konventionen
- UI-Texte Deutsch, Code Englisch. API-Keys über `local.properties` (`GEMINI_API_KEY`).
- Weitere Regeln: `.clinerules` und `docs/KONZEPT.md`.
