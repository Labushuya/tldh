# tl;dh STT Bench

## v0.6.0

Whistle (Cactus Compute) als dritte lokale Engine.

Struktur:

```text
Local  → Vosk Android / whisper.cpp / Whistle → jeweilige lokale Modelle bzw. Laufmodi
Remote → Groq Speech-to-Text                 → whisper-large-v3-turbo / whisper-large-v3
```

Alle Engines laufen gegen dieselbe Benchmark-Auswertung:

- Audioquelle
- Audio-Prep-Profil
- Referenztext
- WER/CER/S/I/D
- History / Markdown-Report

### Whistle

- Modell: `whistle.cact`, 16.9 MB, Apache-2.0, Sprachen en/de/fr/es/it/nl/pl. Download im Engines-Tab von einer gepinnten Hugging-Face-Revision mit SHA-256-Prüfung.
- Engine: das vorkompilierte `needle`-Programm (arm64-v8a) wird beim Build geladen, geprüft und als `libneedle_cli.so` in die APK gepackt (`fetchNeedleRuntime` in `app/build.gradle.kts`). Die App startet es pro Audioabschnitt als eigenen Prozess.
- Laufmodi:
  - **Pausen-Chunking** (empfohlen): die App schneidet an Sprechpausen in Stücke <= 25 s.
  - **Engine-Fenster**: ein Aufruf, die Engine zerlegt selbst in 30-s-Fenster. Nur zum Vergleich, weil dabei im Desktop-Test ganze Sätze fehlten.
  - **Pausen-Chunking · Tiefe 4**: halbe Decoder-Tiefe als Tempo-Experiment.

Hinweis: Groq ist Remote/Cloud. Audios verlassen das Gerät nur nach explizitem API-Key/Remote-Auswahl. Vosk, whisper.cpp und Whistle laufen vollständig auf dem Gerät.

Lizenzen der mitgelieferten bzw. geladenen Drittkomponenten: siehe [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
