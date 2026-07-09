# tl;dh STT Bench Changelog

## 0.4.0

- Added reproducible Audio-Prep profiles for real-world audio benchmarking.
- Added Audio-Prep-Matrix runner for same audio + same reference across multiple prep profiles.
- Added Audio-Prep profile labels to single and matrix Markdown reports.
- Single benchmark now uses the selected Audio-Prep profile.
- Corpus batch keeps using the selected Audio-Prep profile.
- Added deterministic local PCM preprocessing variants: Original, Basic Gate, Normalized, Voice-Band + Basic, Aggressive Gate.

## 0.3.9

- Benchmark progress UI changed to indeterminate status where engines do not expose true progress.
- Added benchmark/batch cancel UI.
- Added Audio-Prep placeholder card.
