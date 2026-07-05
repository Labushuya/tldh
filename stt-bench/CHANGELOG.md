# Changelog

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
