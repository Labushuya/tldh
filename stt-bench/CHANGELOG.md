# Changelog

## 0.2.8 - Audio source clarity, word deviations, non-speech reduction

### Added
- Explicit audio source tracking: external Android share, gold-standard sample, or generated longform audio.
- Shared WhatsApp/Telegram audio now clears stale gold-standard selection and stale reference text automatically.
- Benchmark tab now shows a clear "Audioquelle aktiv / bereit" box with source, file hint and reference status.
- In-app metric explanation for WER, CER and S/I/D.
- Result UI now shows a first-class Abweichungsübersicht: total word errors, S/I/D split, character errors and first word-level diffs.
- Batch UI and Markdown reports now include Wortfehler and S/I/D per sample.
- Conservative non-speech preprocessing before Vosk: basic silence trim / long-pause compression with warning when material was removed.

### Changed
- Batch repeat selector uses full-width responsive buttons (`1× Corpus`, `3× Corpus`, `8× Corpus`, `20× Corpus`) so the `×` no longer wraps under the number.
- Results without a reference text now explicitly say that WER/CER/S/I/D cannot be calculated.
- Release tag moves to `stt-bench-v0.2.8`.
- Workflow and Gradle version defaults move to `0.2.8` / versionCode `208`.

## 0.2.6 - Usability revamp, updater restore, long-run profiles

### Added
- Section-based UI navigation: Start, Modelle, Goldstandard, Benchmark, Ergebnisse, Updates.
- Sticky setup summary at the top of each section with current model, audio source, reference state and busy progress.
- Restored benchmark-specific in-app updater with SHA256 verification, wake-lock guarded download and Android APK installer.
- Benchmark update selector only accepts `stt-bench-vX.Y.Z` releases with `tldh-stt-bench-X.Y.Z.apk` assets.
- Batch repeat profiles: `1×`, `3×`, `8×`, `20×` corpus runs for longer reproducible STT workload comparisons.

### Changed
- Single and batch benchmark completion now routes the user to the Ergebnisbereich instead of leaving the result buried below a long scroll stack.
- Goldstandard area explains short starter samples versus long-run batch profiles more clearly.
- Release tag moves to `stt-bench-v0.2.6`.
- Workflow and Gradle version defaults move to `0.2.6` / versionCode `206`.

## 0.2.5 - Batch corpus benchmarking and Markdown reports

### Added
- Batch-Benchmark card for running all installed gold-standard corpus samples sequentially with the active Vosk model.
- Aggregated batch summary with sample count, average RTF, average WER/CER, speed-pass count and worst WER sample.
- Markdown copy report for full batch results, including per-sample timings, WER/CER and recognized transcripts.
- Markdown copy report for single benchmark runs.
- Partial batch preservation: if a later sample fails, completed results remain visible as a report.

### Changed
- Release tag moves to `stt-bench-v0.2.5`.
- Workflow and Gradle version defaults move to `0.2.5` / versionCode `205`.

## 0.2.4 - FLAC corpus and UI contrast fix

### Fixed
- Accept FLAC/WAV/MP3 as benchmark/reference formats so the built-in gold-standard FLAC corpus is not rejected by the MVP format gate.
- Keep WhatsApp OGG/Opus as the primary product-target format while allowing clean reference corpus formats for WER/CER tests.
- Apply an explicit dark Material color scheme to avoid unreadable default dark text on dark surfaces.
- Set explicit OutlinedTextField colors for text, label, placeholder, cursor, border and container states.
- Use explicit high-contrast button colors for primary benchmark actions.

## 0.2.3 - Built-in gold-standard starter corpus

### Added
- Goldstandard-Testaudios card with a curated CC0 German starter corpus.
- One-tap download for 8 clean German reference audios from `rhasspy/dataset-voice-kerstin`.
- Per-sample download, select, delete actions.
- Selecting a sample automatically sets the audio input and correct reference transcript.
- Corpus clear action for removing all downloaded starter audios.

### Changed
- Manual reference text remains available, but the benchmark app now provides ready-to-use baseline audio/transcript pairs.
- Release tag moves to `stt-bench-v0.2.3`.


## 0.2.2 - Reference text comparison / WER-CER

### Added
- Optional reference text / gold-standard field for each benchmark run.
- Clipboard import button for reference transcripts.
- Automatic WER and CER calculation after Vosk transcription.
- Reference comparison summary in the result UI.
- Expandable view showing reference text vs. recognized text.
- Last-5 benchmark history now stores WER/CER and comparison summary when a reference text was used.
- Reference comparison warnings for weak WER and missing reference words.

### Changed
- Release tag moves to `stt-bench-v0.2.2`.
- Benchmark report can now evaluate speed and recognition accuracy in the same run.

## 0.2.1 - Vosk multi-model benchmark + last-5 history

### Added
- Multi-model Vosk catalog for German benchmark candidates.
- Live model switching without app restart.
- Per-model download/reinstall/delete actions.
- Traffic-light UI for speed, expected accuracy, and phone suitability.
- Reset button to clear the current benchmark result for the next run.
- Benchmark runner now accepts the selected Vosk model dynamically.
- Stores the last 5 benchmark runs locally in the benchmark app.
- Adds an on-demand history UI with timing, model, verdict and transcript preview.

### Changed
- Release tag moves to `stt-bench-v0.2.1`.
- README now documents multi-model testing and model tradeoffs.

## 0.1.3 - API 28 share fix

### Fixed
- Share reader compatibility for `minSdk 28`.
