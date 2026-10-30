# tl;dh STT Bench Changelog

## 0.6.0 - Whistle als dritte lokale Engine
- Whistle (Cactus Compute, 16.9 MB, Apache-2.0) als lokale Engine ergänzt: Local → Whistle → Laufmodus.
- Die `needle`-Engine (arm64-v8a) wird beim Build von einer gepinnten Hugging-Face-Revision geladen, per SHA-256 geprüft und als `libneedle_cli.so` in die APK gepackt.
- Whistle läuft als eigener Prozess: ein nativer Absturz oder Hänger beendet nur den Lauf, Abbrechen beendet den Prozess sofort.
- Das Whistle-Modell wird im Engines-Tab geladen (gepinnte Revision, SHA-256-Prüfung).
- Drei Laufmodi: Pausen-Chunking (Stücke <= 25 s, empfohlen), Engine-Fenster (Vergleich) und Pausen-Chunking mit Decoder-Tiefe 4 (Tempo-Experiment).
- Deutsch-Lock und Wort-Zeitstempel mit Wahrscheinlichkeit pro Wort.
- Neuer `PauseChunkPlanner` schneidet an Sprechpausen statt mitten in Wörtern; Unit-Tests ergänzt.
- Hintergrund: Im Desktop-Vorabtest ließ die Engine-eigene 30-s-Fensterung an einer Fenstergrenze drei Sätze aus (4-min-Datei: 42 fehlende Wörter; mit Pausen-Chunking: 8).

## 0.5.1 - Real-World Worttreue / tl;dh-Wertung
- Ergänzt zusätzlich zur strengen WER/CER eine reale tl;dh-Wertung.
- Normalisiert Low-Impact-Abweichungen wie Zahlformate, Füllwörter, leichte Schreib-/Namensvarianten und einfache Wortformen.
- Zeigt Content-Match, normalisierte WER, bereinigte Low-Impact-Abweichungen und kritische Abweichungen.
- Produktentscheidung nutzt die reale Wertung, wenn Referenztext vorhanden ist.
- Markdown-Report enthält strenge WER/CER und reale tl;dh-Wertung separat.

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
