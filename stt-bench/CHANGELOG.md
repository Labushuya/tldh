# Changelog

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
