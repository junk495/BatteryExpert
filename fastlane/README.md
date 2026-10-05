# Fastlane-Metadaten für F-Droid

Diese Dateien liefern F-Droid die lokalisierte Beschreibung, den Titel und
(optional) Icon + Screenshots für die Store-Seite. F-Droid liest sie automatisch
aus diesem Repo, wenn die App über `fdroiddata` gebaut wird.

## Struktur

```
fastlane/metadata/android/
  en-US/
    title.txt               # App-Name (max. 30 Zeichen)
    short_description.txt   # Kurzbeschreibung (max. 80 Zeichen)
    full_description.txt    # ausführliche Beschreibung
    images/
      icon.png              # 512 x 512 px PNG (erforderlich für F-Droid)
      featureGraphic.png    # optional, 1024 x 500 px
      phoneScreenshots/
        1.png               # Screenshots (Smartphone)
        2.png
        ...
  de/
    title.txt
    short_description.txt
    full_description.txt
    images/                 # optional eigene Bilder für Deutsch
      ...
```

## Noch zu erledigen

1. **Icon** unter `en-US/images/icon.png` ablegen (512×512, PNG, keine WebP).
   Das App-Icon (`app/src/main/res/mipmap-*/ic_launcher.webp`) liegt derzeit nur
   als WebP vor – für F-Droid wird eine hochauflösende PNG-Version benötigt.
2. **Screenshots** unter `en-US/images/phoneScreenshots/` ablegen (1.png, 2.png, …).
3. Optional: dieselben Bilder auch unter `de/images/` hinterlegen, falls die
   deutsche Store-Seite eigene Bilder bekommen soll. Wenn `de/images/` leer
   bleibt, nutzt F-Droid die Bilder aus `en-US`.
