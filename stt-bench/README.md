# tl;dh STT Bench

Separate Benchmark-App im tl;dh-Repository.

## Current release: 0.3.9

### Neu in 0.3.9

- Benchmark-UX bereinigt: ein sichtbarer Laufstatus statt gemischter Top-Progressbar + separater Spinner-Animation.
- Keine irreführende `0%`-Anzeige mehr bei Engine-Läufen ohne echten Progress-Callback.
- Vosk/Whisper-Läufe zeigen Elapsed-Time und indeterminate Progress.
- Einzelbenchmark und Batchbenchmark können aus der UI abgebrochen werden.
- Audio-Prep-Testmatrix als nächster Prüfpfad vorbereitet: Original, Normalisierung, stärkere VAD/Silence-Strategie, High-Pass/Loudness und segmentierte Chunks.

## Release

```text
stt-bench-v0.3.9
tldh-stt-bench-0.3.9.apk
```

Die Produkt-App `app/` wird von diesem Bench-Release nicht verändert.
