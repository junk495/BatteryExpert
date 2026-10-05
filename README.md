# BatteryExpert

Android-App zur Verwaltung von Akku-Daten und zur direkten Steuerung des
**SkyRC MC5000** (4-Slot-Ladegerät) über Bluetooth Low Energy (BLE).

> Nur Android. Entwicklung ausschließlich in Android Studio.

> ⚠️ **Rechtlicher Hinweis:** Dieses Projekt ist ein privates Hobby-Projekt und
> steht in **keiner Verbindung** zu SkyRC Technology Co., Ltd. „SkyRC" und
> „MC5000" sind Marken ihrer jeweiligen Inhaber. Die Nutzung erfolgt
> **ausschließlich auf eigene Verantwortung** – Details unter
> [Haftungsausschluss](#haftungsausschluss).

## Funktionen

- **Zelltypen-Datenbank:** Seriendaten (Datenblatt) je Zelltyp verwalten – Hersteller, Modell,
  Chemie, Kapazität, Lade-/Entladeparameter, NiMH-Delta-Peak, … (Room/SQLite).
- **Zellen-Bestand:** einzelne Zellen einem Zelltyp zuordnen (Herkunft, Status, Lagerort).
- **Live-Monitoring:** Spannung, Strom, Kapazität, Zeit und Innenwiderstand der 4 Slots
  in Echtzeit (inkl. Spannungs-/Stromverlauf als Diagramm).
- **Zell-Bewertung (Test-Tab):** Innenwiderstand prüfen + Kapazitäts-/SOH-Test
  (Schnell- oder genauer Test) → Ampel (ok/beobachten/aussortieren).
- **Steuerung:** Lade-/Entladeparameter direkt an den MC5000 senden (`0x94` + `0x93`).
- **KI-Recherche:** Zelltyp-Daten per Gemini **oder DeepSeek** recherchieren und strukturiert übernehmen.
- **Import/Export:** Daten als JSON sichern/austauschen (optional CSV für Testergebnisse).

## Technik

| Baustein     | Wahl                                  |
|--------------|---------------------------------------|
| Sprache      | Kotlin                                |
| UI           | Jetpack Compose + Material 3          |
| Architektur  | MVVM + Repository + Flow              |
| Datenbank    | Room (SQLite) + KSP                   |
| BLE          | nativ `android.bluetooth.le`          |
| KI           | Gemini API + DeepSeek API              |
| Diagramme    | Vico                                  |
| JSON         | Gson                                  |

`minSdk 26`, `compileSdk`/`targetSdk 34`.

## Voraussetzungen

- Android Studio (aktuelle Version)
- JDK (Android Studio bringt ein JBR mit)
- Für die KI-Recherche: ein API-Key (Gemini oder DeepSeek) – Eingabe in der App (Einstellungen)

## Einrichtung & Build

1. Projekt in Android Studio öffnen.
2. Auf ein BLE-fähiges Android-Gerät (min. Android 8.0 / API 26) installieren.
3. KI-Key in den Einstellungen hinterlegen (Gemini oder DeepSeek).
   (Alternativ als Fallback: `GEMINI_API_KEY` in `local.properties`.)

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
    ble/          Mc5000BleManager, ProtocolCodec, BleDevice, SlotStatus
    db/           Room-Entities (CellType, Battery, TestResult, …), DAOs, AppDatabase
    assessment/   AssessmentLogic (IR/SOH/Empfehlung)
    repository/   Battery-, Ble-, Ai-, ExportImport-, Measurement-, Test-Repository
    ApiKeyStore.kt (Gemini-/DeepSeek-Key)
  ui/
    screens/      BatteryList, BatteryDetail, BatteryEdit, Monitor,
                  ConfigEditor, AiResearch, Settings
    test/         TestScreen (Zell-Bewertung)
    viewmodels/   …
    theme/
  MainActivity.kt
docs/
  KONZEPT.md       Konzept & Architektur
  TESTPROTOKOLL.md Messprotokoll (Schnell-/genauer Test)
  PROMPTS.md       Prompts für die Android-Studio-KI
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
`AssessmentLogicTest`, `ExportImportRepositoryTest`, `ViewModelsTest`.

## Referenzen

- [`skyrc-mc-rs`](https://github.com/rssdev10/skyrc-mc-rs) (MIT) – Rust-Implementierung des MC5000-Protokolls
- [`skyrc-mc3000`](https://github.com/kolinger/skyrc-mc3000) (GPL-3.0) – Python-BLE-Monitor
- [`skyrc.com/MC5000`](https://www.skyrc.com/MC5000) – offizielle Produktseite

## Haftungsausschluss

- **Keine Verbindung zum Hersteller:** BatteryExpert ist ein unabhängiges,
  privates Hobby-Projekt. Es wird nicht von SkyRC Technology Co., Ltd. entwickelt,
  unterstützt, gesponsert oder freigegeben. „SkyRC" und „MC5000" sind Marken
  ihrer jeweiligen Inhaber und werden hier nur zur Beschreibung der Kompatibilität
  genannt.
- **Reiner Hobby-Zweck:** Die App entsteht aus privatem Interesse an der Technik
  und ist für den eigenen, nicht-kommerziellen Gebrauch gedacht.
- **Keine Haftung:** Die Software wird ohne jegliche Gewährleistung bereitgestellt
  (siehe GPL-3.0). Der Umgang mit Akkus – insbesondere Lithium-Ionen und NiMH –
  sowie das Laden/Entladen ist grundsätzlich mit Risiken verbunden (u. a. Brand-
  und Explosionsgefahr bei fehlerhaften Zellen, falschen Parametern oder defekter
  Hardware). Schäden am Ladegerät, an Akkus oder anderem Eigentum sowie Personen-
  und Folgeschäden liegen nicht in der Verantwortung der Autoren.
- **Nutzung auf eigene Verantwortung:** Jede Nutzung erfolgt vollständig auf
  eigenes Risiko. Wer die App einsetzt, bestätigt damit, Funktionsweise und
  Risiken zu verstehen und verantwortungsvoll zu handeln (geeignete Umgebung,
  Brandschutz, Beaufsichtigung, korrekte Lade-/Entladeparameter).

## Lizenz

BatteryExpert – Verwaltung von Akku-Daten und Steuerung des SkyRC MC5000
Copyright (C) 2026 junk495

Dieses Programm ist freie Software: Sie können es unter den Bedingungen der
GNU General Public License, wie von der Free Software Foundation veröffentlicht,
weiterverbreiten und/oder modifizieren, gemäß Version 3 der Lizenz.

Die Veröffentlichung dieses Programms erfolgt in der Hoffnung, dass es Ihnen von
Nutzen sein wird, aber OHNE IRGENDEINE GARANTIE, sogar ohne die implizite Garantie
der MARKTREIFE oder der VERWENDBARKEIT FÜR EINEN BESTIMMTEN ZWECK. Details finden
Sie in der GNU General Public License.

Sie sollten ein Exemplar der GNU General Public License zusammen mit diesem
Programm erhalten haben. Falls nicht, siehe <https://www.gnu.org/licenses/>.

Vollständiger Lizenztext: [LICENSE](LICENSE) · SPDX: `GPL-3.0-only`

