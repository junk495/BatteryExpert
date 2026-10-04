# BatteryExpert – Testprotokoll (Zell-Bewertung)

Ziel: eine standardisierte, wiederholbare **Alterungsbewertung** je Zelle –
über Innenwiderstand (IR) und Kapazität (SOH).

> Grundprinzip: **Vergleichbarkeit schlägt absolute Genauigkeit.**
> Alle Zellen werden mit derselben Rate gemessen und relativ zueinander
> (bzw. gegen eine frische Referenzzelle) bewertet.

---

## 1. Grundregeln (verbindlich)

1. **Nur vollständig durchgeführte Tests zählen.** Ein Test ist nur gültig,
   wenn Lade- **und** Entladevorgang bis „Completed" gelaufen sind.
   Halbvolle Zelle laden liefert **keine** Aussage.
2. **Entladekapazität ist der Maßstab** (nicht Ladekapazität – die ist durch
   CV-Verluste überhöht).
3. **Konsistente Bedingungen:** gleiche Ströme, gleiche Cutoff-Spannung, vorher voll laden.
4. **Nur Datenbank-Zellen testen** (Referenz nötig + Ergebnis-Zuordnung).

---

## 2. MC5000-Hardwaregrenzen

| Richtung | Max. Strom |
|---|---|
| Laden    | 5 A        |
| Entladen | **2 A**    |

→ Entladen ist die begrenzende Größe. Ein 3000-mAh-Akku entlädt mit 2 A = 0,67 C.

---

## 3. Protokoll A – Genauer Test (Referenz, ~5–6 h/Zelle)

- Laden: **0,5 C** bis Abschaltung (Li-Ion CC/CV 4,2 V + Terminierungsstrom ~0,05 C; NiMH ΔU).
- Ruhe: 10 min (optional).
- Entladen: **0,2 C** bis Cutoff (Li-Ion 3,0 V, NiMH 0,9 V).
- Messen: **Entladekapazität** → `SOH = gemessen / Nennkapazität`.

→ Zweck: genaue Vergleichswerte, z. B. für die beste/frische Referenzzelle.

---

## 4. Protokoll B – Schnelltest (Screening, ~3 h/Zelle)

- Laden: **1 C** (bzw. so hoch wie sinnvoll, max. 5 A).
- Entladen: **2 A** (max.), Cutoff je Chemie.
- Messen: **Entladekapazität** → relativer SOH.

→ Zweck: schnelles Screening von vielen Zellen. Bei Li-Ion nur ~2–5 % weniger
  Kapazität als Protokoll A – für die Aussortierung völlig ausreichend.
→ Umsetzung im MC5000 als **Cycle C→D, 1 Zyklus** (lädt automatisch voll und entlädt).

---

## 5. Zeitbudget (30 Zellen, 4 Slots)

| Protokoll      | pro Zelle | 30 Zellen (8 Durchgänge) |
|----------------|-----------|---------------------------|
| Schnelltest    | ~3 h      | ~24 h (über Nacht)        |
| Genauer Test   | ~5–6 h    | ~40–48 h                  |

Empfehlung: erst **IR-Schnellscreen** (Minuten), dann **Schnelltest** für alle,
**genauen Test** nur für Grenzfälle oder die Referenzzelle.

---

## 6. Erstbewertung (IR-Schnellscreen)

- Kurzer Messvorgang (MC5000 misst IR beim Ladebeginn, nicht im Leerlauf):
  kurz mit ~100 mA starten, IR lesen, stoppen.
- Bewertung über das Verhältnis **gemessen / Soll-IR** (`BatteryEntity.internalResistanceMOhm`):

| Verhältnis | Empfehlung      |
|------------|-----------------|
| < 1,5×     | **OK** (grün)   |
| 1,5 – 2×   | **Beobachten** (gelb) |
| > 2×       | **Aussortieren** (rot) |

---

## 7. SOH-Ampel (nach Kapazitätstest)

| SOH       | Bewertung             |
|-----------|-----------------------|
| > 90 %    | **OK** (grün)         |
| 80 – 90 % | **Beobachten** (gelb) |
| < 80 %    | **Aussortieren** (rot)|

(Richtwerte, in der App zentral konfigurierbar.)

---

## 8. Testströme in der Zelldatenbank

`BatteryEntity` speichert je Zelle vier Ströme:
- `fastChargeCurrentMa`, `fastDischargeCurrentMa` – Schnelltest.
- `slowChargeCurrentMa`, `slowDischargeCurrentMa` – genauer Test.

Defaults automatisch aus `capacityMah` (Schnelltest ≈ 1 C laden / 2 A entladen,
genauer Test ≈ 0,5 C laden / 0,2 C entladen), pro Zelle überschreibbar.

---

## 9. Ablauf im Test-Tab

1. Zelle(n) aus der Datenbank den Slots 1–4 zuordnen.
2. Erstbewertung: IR messen → Empfehlung.
3. Testart je Slot wählen (Schnell/Genau).
4. Cycle C→D starten, Fortschritt beobachten.
5. Ergebnis (SOH + IR + Ampel) als `TestResultEntity` speichern.
