# Changelog

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
