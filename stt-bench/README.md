# tl;dh STT Bench

## v0.5.0

Remote-Layer für Groq Speech-to-Text.

Neue Struktur:

```text
Local  → Vosk Android / whisper.cpp → jeweilige lokale Modelle
Remote → Groq Speech-to-Text      → whisper-large-v3-turbo / whisper-large-v3
```

Groq läuft gegen dieselbe Benchmark-Auswertung wie Local:

- Audioquelle
- Audio-Prep-Profil
- Referenztext
- WER/CER/S/I/D
- History / Markdown-Report

Hinweis: Groq ist Remote/Cloud. Audios verlassen das Gerät nur nach explizitem API-Key/Remote-Auswahl.
