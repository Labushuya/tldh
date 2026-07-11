# tl;dh STT Bench Changelog

## 0.5.0
- Neue Hierarchie: Local/Remote → Engine/Provider → Model.
- Groq Speech-to-Text als Remote-Provider ergänzt.
- Groq-Modelle: whisper-large-v3-turbo und whisper-large-v3.
- Groq-Benchmark nutzt dieselbe Referenztext-, WER-, CER- und S/I/D-Auswertung wie lokale Engines.
- Remote-Upload erfolgt bewusst über lokal vorbereitetes 16 kHz Mono WAV mit gewähltem Audio-Prep-Profil.
- Groq API-Key und Prompt im Engines-/Modelle-Tab konfigurierbar.

## 0.4.0
- Audio-Prep-Matrix für Original, Basic Gate, Normalisierung, Voice-Band und aggressives Gate.
- Einzelbenchmark und Matrix nutzen gewähltes Audio-Prep-Profil.
- Matrix-Report für WER/CER/S/I/D pro Profil.
