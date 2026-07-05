# Changelog

## 0.2.0 - Vosk multi-model benchmark

### Added
- Multi-model Vosk catalog for German benchmark candidates.
- Live model switching without app restart.
- Per-model download/reinstall/delete actions.
- Traffic-light UI for speed, expected accuracy, and phone suitability.
- Reset button to clear the current benchmark result for the next run.
- Benchmark runner now accepts the selected Vosk model dynamically.

### Changed
- Release tag moves to `stt-bench-v0.2.0`.
- README now documents multi-model testing and model tradeoffs.

## 0.1.3 - API 28 share fix

### Fixed
- Share reader compatibility for `minSdk 28`.
