# BatteryExpert

Android-App zur Verwaltung von Akku-Daten und zur direkten Steuerung des
**SkyRC MC5000** (4-Slot-Ladegerät) über Bluetooth Low Energy (BLE).

> Nur Android. Entwicklung ausschließlich in Android Studio.

## Funktionen

- **Akku-Datenbank:** Akkus (Marke, Modell, Chemie, Kapazität, Größe, Innenwiderstand …)
  inkl. Lade-/Entladeprofilen und Messreihen lokal verwalten (Room/SQLite).
- **Live-Monitoring:** Spannung, Strom, Temperatur, Kapazität, Zeit und Innenwiderstand
  der 4 Slots in Echtzeit anzeigen (inkl. Spannungsverlauf als Diagramm).
- **Steuerung:** Lade-/Entladeparameter direkt an den MC5000 senden
  (Konfig `0x94` + Start/Stopp `0x93`).
- **KI-Recherche:** Akku-Daten per Gemini recherchieren und strukturiert in die App übernehmen.
- **Import/Export:** Batterien (inkl. Profile + Messungen) als JSON sichern/austauschen.

## Technik

| Baustein     | Wahl                                  |
|--------------|---------------------------------------|
| Sprache      | Kotlin                                |
| UI           | Jetpack Compose + Material 3          |
| Architektur  | MVVM + Repository + Flow              |
| Datenbank    | Room (SQLite) + KSP                   |
| BLE          | nativ `android.bluetooth.le`          |
| KI           | Gemini API (`generativeai`)           |
| Diagramme    | Vico                                  |
| JSON         | Gson                                  |

`minSdk 26`, `compileSdk`/`targetSdk 34`.

## Voraussetzungen

- Android Studio (aktuelle Version)
- JDK (Android Studio bringt ein JBR mit)
- Für die KI-Recherche: ein Gemini-API-Key

## Einrichtung & Build

1. Projekt in Android Studio öffnen.
2. (Optional) Für die KI-Recherche in `local.properties` eintragen:
   ```properties
   GEMINI_API_KEY=DEIN_SCHLUESSEL
   ```
3. Auf ein BLE-fähiges Android-Gerät (min. Android 8.0 / API 26) installieren.

Kommandozeile (Windows):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug        # APK bauen
.\gradlew.bat testDebugUnitTest    # Unit-Tests
```

## Projektstruktur

```
app/src/main/java/com/batteryexpert/
  data/
    ble/         Mc5000BleManager, ProtocolCodec, BleDevice, SlotStatus
    db/          Room-Entities, DAOs, AppDatabase
    repository/  BatteryRepository, BleRepository, AiRepository, …
  ui/
    screens/     BatteryList, BatteryDetail, BatteryEdit, Monitor,
                 ConfigEditor, AiResearch, Settings
    viewmodels/  …
    theme/
  MainActivity.kt
docs/
  KONZEPT.md     Konzept & Architektur
  PROMPTS.md     Prompts für die Android-Studio-KI
```

## BLE-Protokoll (MC5000)

Das Protokoll ist reverse-engineered.
Referenz-Implementierung: [`rssdev10/skyrc-mc-rs`](https://github.com/rssdev10/skyrc-mc-rs).

- Service `0000ffe0-0000-1000-8000-00805f9b34fb`, Charakteristik `0000ffe1-…`
  (READ | WRITE_WITHOUT_RESPONSE | NOTIFY)
- Paket: `0x0F | Länge | Kommando | Daten… | Prüfsumme` (Prüfsumme = Summe mod 256)
- Lesen: `0x91` (Status) · Schreiben: `0x94` (Konfig) + `0x93` (Start/Stopp)

Details: [`docs/KONZEPT.md`](docs/KONZEPT.md).

## Tests

`.\gradlew.bat testDebugUnitTest` – u. a. `ProtocolCodecTest`, `AppDatabaseTest`,
`ExportImportRepositoryTest`, `ViewModelsTest`.

## Referenzen

- [`skyrc-mc-rs`](https://github.com/rssdev10/skyrc-mc-rs) (MIT) – Rust-Implementierung des MC5000-Protokolls
- [`skyrc-mc3000`](https://github.com/kolinger/skyrc-mc3000) (GPL-3.0) – Python-BLE-Monitor
- [`skyrc.com/MC5000`](https://www.skyrc.com/MC5000) – offizielle Produktseite

## Lizenz

Copyright (c) 2026 junk495

Lizenziert unter der [PolyForm Noncommercial License 1.0.0](LICENSE) – freie, nicht-kommerzielle Nutzung.

