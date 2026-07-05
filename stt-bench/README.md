<p align="center">
  <img src="docs/brand/banner.png" alt="tl;dh STT Bench" width="100%">
</p>

# tl;dh STT Bench

Separate Android-Benchmark-App für die STT-Engine-Entscheidung von **tl;dh**.

Diese App ist bewusst **nicht** die Haupt-App. Sie dient nur dazu, deutsche WhatsApp-/Telegram-Audios gegen lokale STT-Kandidaten zu messen, damit tl;dh nicht mit Benchmark-Code, Experimenten oder halbgaren Engines belastet wird.

## Ziel

Benchmark mit echten geteilten Audios:

| Audio | Zielzeit |
|---:|---:|
| 15 Sekunden | ≤ 15 Sekunden |
| 1 Minute | ≤ 30 Sekunden |
| 3 Minuten | ≤ 2 Minuten |

Gemessen werden:

- Audio-Dauer
- Decode-Zeit
- Modell-Ladezeit
- STT-Zeit
- Gesamtzeit
- RTF (`processing / audio`)
- Transkriptqualität anhand echter Audio
- vollständiges erkanntes Exzerpt mit Zeitstempeln

## Engine in v0.1.0

Erster Kandidat:

- **Vosk small German** `vosk-model-small-de-0.15`
- Offline
- Android-tauglich
- Ziel: schneller Handy-Fast-Mode für kurze deutsche Sprachnotizen

Die App lädt das Modell auf Wunsch aus der offiziellen Vosk-Model-Quelle herunter und speichert es lokal im App-Speicher.

## Flow

```text
WhatsApp Audio teilen
→ tl;dh STT Bench auswählen
→ Modell installieren, falls nötig
→ Benchmark starten
→ Ergebnis prüfen
→ Transkript-Klapptext mit Zeitstempeln gegenhören
```

## Datenschutz

- Keine Cloud-STT.
- Modell-Download nur auf Nutzerklick.
- Geteilte Audio wird nur lokal verarbeitet.
- App ist eine separate Benchmark-App und schreibt keine tl;dh-Haupt-App-Daten.

## Ergebnisnutzung

Die Ergebnisse entscheiden, ob Vosk als Phone-Fast-Mode taugt oder ob wir sherpa-onnx/whisper.cpp/LAN-Accelerator priorisieren.
