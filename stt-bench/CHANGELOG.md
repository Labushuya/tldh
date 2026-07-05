# Changelog

## [0.1.1] - 2026-07-05

### Fixed
- Make benchmark APK releases and debug artifacts unmistakably separate from the main tl;dh app.
- Keep the benchmark package and app label isolated as `dev.bitsbots.tldhbench` / `tl;dh STT Bench`.

## [0.1.0] - 2026-07-05

### Added
- Separate Android benchmark app `tl;dh STT Bench` with package `dev.bitsbots.tldhbench`.
- Share target for WhatsApp/Telegram audio without touching the main tl;dh app.
- Vosk small German benchmark path.
- Model download/install helper for `vosk-model-small-de-0.15`.
- Decode → 16 kHz mono PCM preparation.
- Benchmark metrics: decode time, model load time, STT time, total time, RTF, pass/fail target.
- Expandable full transcript excerpt with timestamps for verification.
