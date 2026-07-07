# tl;dh STT Bench


## v0.3.6 — engine-specific model tabs and install-safe version bump

This release keeps the first executable whisper.cpp path and fixes the model UX around it. The Modelle tab is now engine-specific: Vosk Android shows Vosk model folders, whisper.cpp shows Whisper ggml models. The version is bumped to 0.3.6 so it can be installed over corrected 0.3.4 builds without uninstalling first.

- Vosk remains available as the speed baseline.
- whisper.cpp can now be selected and executed for single benchmarks and batch corpus runs.
- tiny/base/small Whisper models can be compared against the exact same audio/reference/WER/CER pipeline as Vosk.
- Whisper reports currently contain a full transcript but no word/segment timestamps.
- Recommended first test order on phone: `Whisper tiny` → `Whisper base` → only then `Whisper small` if storage/battery/time are acceptable.

Separate Android benchmark app for testing local German STT engines before anything is integrated into the main `tl;dh` app.

## Current release: 0.3.6

Focus: install-safe fix release after v0.3.6. The goal is still a fair measured comparison between Vosk and whisper.cpp on the same real WhatsApp audios and gold-standard references, with clear separation of model pools.

## Features

- Separate Android package: `dev.bitsbots.tldhbench`.
- Share target for WhatsApp/Telegram audio.
- Engine-specific model catalog with live download/delete/cleanup/switching: Vosk models for Vosk Android, Whisper ggml models for whisper.cpp.
- Model traffic lights for speed, expected accuracy, and phone suitability.
- Benchmark timings: decode, model load, STT, total, RTF.
- Expanded built-in gold-standard corpus with curated German FLAC reference audios from `rhasspy/dataset-voice-kerstin`.
- One-tap corpus download inside the app.
- Selecting a sample automatically sets audio input and reference text.
- Optional manual reference text / gold-standard field remains available.
- Automatic WER/CER/S-I-D calculation after each benchmark when a reference is provided.
- First-class deviation overview with total word errors, substitutions, insertions, deletions and first word-level diffs.
- Last 5 benchmark runs stored locally inside the benchmark app.
- Current run reset button; history reset remains separate.
- Explicit dark theme/text-field colors so reference fields remain readable.
- Section-based UI: Start, Engines, Modelle, Goldstandard, Benchmark, Ergebnisse, Updates.
- Modelle tab now follows the active engine instead of showing Vosk models while whisper.cpp is active.
- Responsive full-width action buttons with minimum height and centered labels so text does not shift or collapse on phone screens.
- Generated longform WAV profiles: ~30 seconds, ~90 seconds, and ~4 minutes, each with an automatically built reference transcript.
- Batch-benchmark all installed gold-standard samples with the active Vosk or whisper.cpp model.
- Batch repeat profiles: 1×, 3×, 8×, 20× corpus repeats for aggregate model reports.
- Benchmark-specific in-app updater for `stt-bench-vX.Y.Z` release APKs.
- Copy single-run or batch results as Markdown for handover/comparison, including word-error, S/I/D metrics, and tl;dh product-readiness decisions.

## Engine roadmap in v0.3.x

The app now has a dedicated **Engines** section. Vosk and whisper.cpp can be selected independently so the same audios can be measured against the same reference text. The current state is:

- **Vosk Android**: executable speed baseline.
- **whisper.cpp**: first executable on-device comparison path in v0.3.6; uses downloaded `ggml-*.bin` models and the same reference-comparison pipeline.
- **sherpa-onnx**: planned second non-Vosk mobile candidate after a suitable German model is selected.
- **LAN/Tower Whisper**: later local-network quality mode for longer or important audios.

The benchmark result now evaluates each run against tl;dh product-readiness thresholds. Roughly: <=15% WER can become a product candidate, <=25% WER may be usable with guardrails, higher values are treated as preview-only or blocked.

## Built-in gold-standard corpus

The app can download a curated subset of `rhasspy/dataset-voice-kerstin` on demand. The dataset provides native German speech, transcripts via `metadata.csv`, and is CC0-licensed. v0.2.8 expands the pool beyond the original starter clips so realistic longform profiles can be generated from multiple real utterances.

The audio files are not bundled into the APK. They are downloaded on demand to keep the APK small and to keep the source transparent.

## Reference comparison

Either select a built-in sample or paste the correct transcript into the reference field before starting the benchmark. The app then calculates:

- WER: word error rate.
- CER: character error rate.
- Word substitutions / insertions / deletions.
- First word-level differences, so the user can see which words were replaced, added or missed.
- A rough label: very good, usable, critical, weak.

This is intended for the built-in starter corpus, Common Voice, self-made WhatsApp gold-standard clips, or any audio where the correct transcript is known.

## Shared audio readiness

When an audio is shared from WhatsApp/Telegram into the benchmark app, the Benchmark section now shows it as an explicit external Android-share source. A new shared audio clears stale gold-standard/longform state and clears any previous reference text, so the next Einzelbenchmark uses the newly shared file. For own audios, WER/CER/S-I-D only appear when the correct transcript is manually pasted into the reference field before running the benchmark.

## Non-speech reduction

Before Vosk receives PCM, the app now performs a conservative local preprocessing pass: 16 kHz mono conversion, basic silence trimming and long-pause compression. If material was removed, the result warning states roughly how many seconds/percent were removed. The timing target still uses the original input duration, because product usefulness is measured against the user's actual voice-message length.

## Android safety fixes in 0.2.9

- The top-level layout now respects Android status/camera and navigation areas, with extra bottom breathing room for gesture/soft-button devices.
- The benchmark target class now covers audios up to 380 seconds. For 180-380 seconds, the hard target is faster-than-listening (`RTF <= 1.0`).
- Zamia model installation now searches nested extracted model directories and normalizes them if the ZIP layout differs from the expected root.
- The very large TUDA model remains downloadable for inspection, but Android benchmark execution is blocked by a crash guard because native Vosk loading can terminate the app before Kotlin can catch an exception.

## Failed model cleanup

Every model card now shows a cleanup action even if the model is not currently recognized as installed. This intentionally covers failed/partial ZIP extractions: use **Lokale Reste bereinigen** for the affected model, then start the download again.

The installer also clears previous partial state before each new download and uses a more permissive plausibility check for older/repackaged Vosk models. If a model passes the local layout check but still cannot load, the native Vosk model loader will report the final benchmark-time error.

## Privacy

No telemetry. No cloud STT. The reference text, downloaded starter samples, and last-5 history are stored locally in the benchmark app only.


## Longform profiles

After downloading the gold-standard corpus, the **Benchmark** section can generate single WAV test audios of approximately 30 seconds, 90 seconds, or 4 minutes. These files are built locally from real downloaded CC0 reference recordings with short pauses between utterances. The app also creates the matching reference transcript automatically, so WER/CER still compares the recognized transcript of the active STT engine against the intended text.

## Batch reports

After downloading one or more built-in gold-standard samples and installing a Vosk or Whisper model, use **Batch starten** to run the active model against every installed reference sample. Choose `1×`, `3×`, `8×`, or `20×` to run longer reproducible workloads. The app summarizes average RTF, WER/CER, speed-pass count, worst WER sample, and per-sample timings. Use **Batch-Report kopieren** to paste the complete Markdown report into GitHub, notes, or the next ChatGPT handover.


## In-app updates

The benchmark app includes its own manual updater. It only considers GitHub releases tagged `stt-bench-vX.Y.Z` and assets named `tldh-stt-bench-X.Y.Z.apk`. It does not install or select main `tl;dh` APKs. Downloads are SHA256-checked and guarded with a wake-lock before the Android package installer is opened.
