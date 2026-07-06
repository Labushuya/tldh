# tl;dh STT Bench

Separate Android benchmark app for testing local German STT engines before anything is integrated into the main `tl;dh` app.

## Current release: 0.2.7

Focus: Vosk multi-model testing with responsive section-based UI, restored benchmark updater, expanded built-in gold-standard corpus, true generated longform profiles, WER/CER comparison, batch reports, and Markdown export.

## Features

- Separate Android package: `dev.bitsbots.tldhbench`.
- Share target for WhatsApp/Telegram audio.
- Vosk German model catalog with live download/delete/switching.
- Model traffic lights for speed, expected accuracy, and phone suitability.
- Benchmark timings: decode, model load, STT, total, RTF.
- Expanded built-in gold-standard corpus with curated German FLAC reference audios from `rhasspy/dataset-voice-kerstin`.
- One-tap corpus download inside the app.
- Selecting a sample automatically sets audio input and reference text.
- Optional manual reference text / gold-standard field remains available.
- Automatic WER and CER calculation after each benchmark when a reference is provided.
- Expandable reference vs. recognized transcript view.
- Last 5 benchmark runs stored locally inside the benchmark app.
- Current run reset button; history reset remains separate.
- Explicit dark theme/text-field colors so reference fields remain readable.
- Section-based UI: Start, Modelle, Goldstandard, Benchmark, Ergebnisse, Updates.
- Responsive full-width action buttons so labels do not shift or collapse on phone screens.
- Generated longform WAV profiles: ~30 seconds, ~90 seconds, and ~4 minutes, each with an automatically built reference transcript.
- Batch-benchmark all installed gold-standard samples with the active model.
- Batch repeat profiles: 1×, 3×, 8×, 20× corpus repeats for aggregate model reports.
- Benchmark-specific in-app updater for `stt-bench-vX.Y.Z` release APKs.
- Copy single-run or batch results as Markdown for handover/comparison.

## Built-in gold-standard corpus

The app can download a curated subset of `rhasspy/dataset-voice-kerstin` on demand. The dataset provides native German speech, transcripts via `metadata.csv`, and is CC0-licensed. v0.2.7 expands the pool beyond the original starter clips so realistic longform profiles can be generated from multiple real utterances.

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


## Longform profiles

After downloading the gold-standard corpus, the **Benchmark** section can generate single WAV test audios of approximately 30 seconds, 90 seconds, or 4 minutes. These files are built locally from real downloaded CC0 reference recordings with short pauses between utterances. The app also creates the matching reference transcript automatically, so WER/CER still compares the recognized Vosk transcript against the intended text.

## Batch reports

After downloading one or more built-in gold-standard samples and installing a Vosk model, use **Batch starten** to run the active model against every installed reference sample. Choose `1×`, `3×`, `8×`, or `20×` to run longer reproducible workloads. The app summarizes average RTF, WER/CER, speed-pass count, worst WER sample, and per-sample timings. Use **Batch-Report kopieren** to paste the complete Markdown report into GitHub, notes, or the next ChatGPT handover.


## In-app updates

The benchmark app includes its own manual updater. It only considers GitHub releases tagged `stt-bench-vX.Y.Z` and assets named `tldh-stt-bench-X.Y.Z.apk`. It does not install or select main `tl;dh` APKs. Downloads are SHA256-checked and guarded with a wake-lock before the Android package installer is opened.
