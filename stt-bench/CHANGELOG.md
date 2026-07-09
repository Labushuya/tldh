# v0.3.9 — Benchmark Control + Audio Prep

- Version: `0.3.9 / 309`, Release: `stt-bench-v0.3.9`, APK: `tldh-stt-bench-0.3.9.apk`.
- Einzelbenchmark-Status bereinigt: keine doppelte Progress-/Spinner-Mischung mehr.
- Benchmark-Fortschritt zeigt bewusst keinen irreführenden `0%`-Wert mehr, wenn die native Engine keinen echten Progress-Callback liefert.
- Vosk und whisper.cpp zeigen während des STT-Laufs einen indeterminate Laufstatus mit Elapsed-Time.
- Einzel- und Batch-Benchmarks besitzen eine Abbrechen-Aktion.
- Hinweis: Native STT-Runner können nicht immer hart unterbrochen werden; UI-State wird aber bereinigt und Whisper-Runtime-Reset bleibt verfügbar.
- Neuer Audio-Prep-Hinweis im Benchmark-Tab: nächster Messpfad ist eine vergleichbare Preprocessing-Matrix für Real-Audios.
- Haupt-App `app/` bleibt unverändert.
