# tl;dh STT Bench

Separate Android benchmark app for testing local German STT engines before anything is integrated into the main `tl;dh` app.

## Current release: 0.2.5

Focus: Vosk multi-model testing with built-in gold-standard starter corpus, WER/CER reference comparison, FLAC reference-audio support, fixed dark UI contrast, batch corpus runs, and Markdown benchmark reports.

## Features

- Separate Android package: `dev.bitsbots.tldhbench`.
- Share target for WhatsApp/Telegram audio.
- Vosk German model catalog with live download/delete/switching.
- Model traffic lights for speed, expected accuracy, and phone suitability.
- Benchmark timings: decode, model load, STT, total, RTF.
- Built-in gold-standard starter corpus with 8 curated German FLAC reference audios from `rhasspy/dataset-voice-kerstin`.
- One-tap corpus download inside the app.
- Selecting a sample automatically sets audio input and reference text.
- Optional manual reference text / gold-standard field remains available.
- Automatic WER and CER calculation after each benchmark when a reference is provided.
- Expandable reference vs. recognized transcript view.
- Last 5 benchmark runs stored locally inside the benchmark app.
- Current run reset button; history reset remains separate.
- Explicit dark theme/text-field colors so reference fields remain readable.
- Batch-benchmark all installed gold-standard samples with the active model.
- Copy single-run or batch results as Markdown for handover/comparison.

## Built-in gold-standard starter corpus

The app can download a small curated subset of `rhasspy/dataset-voice-kerstin` on demand. The dataset provides native German speech, transcripts via `metadata.csv`, and is CC0-licensed.

The audio files are not bundled into the APK. They are downloaded on demand to keep the APK small and to keep the source transparent.

## Reference comparison

Either select a built-in sample or paste the correct transcript into the reference field before starting the benchmark. The app then calculates:

- WER: word error rate.
- CER: character error rate.
- Word substitutions / insertions / deletions.
- A rough label: very good, usable, critical, weak.

This is intended for the built-in starter corpus, Common Voice, self-made WhatsApp gold-standard clips, or any audio where the correct transcript is known.

## Privacy

No telemetry. No cloud STT. The reference text, downloaded starter samples, and last-5 history are stored locally in the benchmark app only.


## Batch reports

After downloading one or more built-in gold-standard samples and installing a Vosk model, use **Batch starten** to run the active model against every installed reference sample. The app summarizes average RTF, WER/CER, speed-pass count, worst WER sample, and per-sample timings. Use **Batch-Report kopieren** to paste the complete Markdown report into GitHub, notes, or the next ChatGPT handover.
