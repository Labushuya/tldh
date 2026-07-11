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


## v0.5.1 - Real-World Worttreue / tl;dh-Wertung

- Ergänzt zusätzlich zur strengen WER/CER eine reale tl;dh-Wertung.
- Normalisiert Low-Impact-Abweichungen wie Zahlformate, Füllwörter, leichte Schreib-/Namensvarianten und einfache Wortformen.
- Zeigt Content-Match, normalisierte WER, bereinigte Low-Impact-Abweichungen und kritische Abweichungen.
- Produktentscheidung nutzt die reale Wertung, wenn Referenztext vorhanden ist.
- Markdown-Report enthält strenge WER/CER und reale tl;dh-Wertung separat.

