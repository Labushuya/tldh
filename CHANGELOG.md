# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]


## [0.3.0] - 2026-07-04

### Added
- Add first local transcription spike using Android on-device file recognition when available on the device.
- Add PCM preparation pipeline for shared audio: decode via Android media stack, convert to 16 kHz mono PCM, then pass a file descriptor to the local recognizer.
- Add transcript-aware result output with copyable transcript and compact technical details.

### Changed
- Reduce result-screen text density by making technical diagnostics secondary.
- Increase TL;DR contrast with a brighter dedicated summary card.
- Keep duration guardrails as the safety gate before local transcription attempts.

### Known limitations
- v0.3.0 is a spike: transcription depends on Android on-device recognition support on the actual device. If unavailable or unreliable, tl;dh falls back to the validated audio-ingest/guardrail output instead of pretending a transcript exists.
- Native `whisper.cpp` integration remains the next hardening step after this device-level local transcription spike.

## [0.2.5] - 2026-07-04

### Added
- Add new `Long story, short.` banner/logo assets to the repository and app resources.
- Add guarded update downloads with a foreground keep-screen-on state and a partial wake lock.
- Add v0.3.0 Whisper spike planning document based on the validated 4:45 min WhatsApp OGG_OPUS Share protocol.

### Changed
- Shift the in-app color direction toward the deep magenta `#a50b5e` brand accent.
- Add more bottom breathing room to the scrollable mobile UI so dense result cards do not sit flush against the display bottom.
- Make update-download interruption messages more specific and actionable.


## [0.2.4] - 2026-07-04

### Fixed
- Prominently surfaces duration guardrail warnings directly below the TL;DR card.
- Includes warnings/guardrails in the copied Share protocol output.
- Adds explicit warning key points when audio duration exceeds the 3-minute warning threshold.

## [0.2.3] - 2026-07-04

### Added
- Add audio duration probing via Android media metadata before the transcription spike.
- Add duration guardrails: 3-minute warning, 10-minute soft limit warning, and 15-minute hard limit rejection.
- Add unit tests for long-audio warnings, hard-duration rejection, and duration formatting.

### Changed
- Include duration and duration-policy diagnostics in the share-result output.
- Improve GitHub updater HTTP 404 messaging for private or non-public release repositories.

## [0.2.2] - 2026-07-04

### Changed
- Collapse release model from split `offline`/`updater` flavors into one single APK: `tldh-<version>.apk`.
- Keep the app fully offline-capable while including a manual in-app update checker in the same APK.
- Update GitHub Actions to build, sign, checksum and publish one APK per SemVer release.
- Update stable release selection to require exact `tldh-<version>.apk` assets and ignore legacy split-flavor APKs.

### Fixed
- Fix Kotlin regex escaping in SHA256 checksum parsing.
- Remove duplicate flavor-specific `ApkInstaller` source-set risk by using one main implementation.
- Move `FileProvider` configuration into the single main app manifest.

## [0.2.1] - 2026-07-04

### Added
- Add manual stable in-app updater MVP.
- Add GitHub release client with SHA256 verification and stable APK selection.

### Changed
- Keep core app fully offline while allowing optional manual update checks when internet is available.

### Fixed
- Reprocess shared audio intents delivered through `onNewIntent` so sharing a WhatsApp voice note into an already-running app updates the UI.
- Build SemVer-named signed APKs through the release workflow instead of relying on CI debug artifacts for install testing.
- Allow release workflows to inject `VERSION_NAME` and `VERSION_CODE` into Gradle builds.

## [0.2.0] - 2026-07-04

### Added
- Harden audio ingest policy and Ogg/Opus detection.
- Add 32 KB header probe and 50 MB MVP file-size policy.
- Add audio ingest policy unit tests.

## [0.1.0] - 2026-07-03

### Added
- Initial `tl;dh` Android project bootstrap.
- Android Share Target for `audio/*`, `application/ogg`, and defensive `application/octet-stream`.
- Kotlin/Compose UI in the pulsating-purpur brand direction.
- Session manager with orphan cleanup on startup.
- Audio metadata inspection and Ogg/Opus header detection.
- Fake summarizer to prove the end-to-end share/result/copy flow.
- Updater-domain model: SemVer, stable release selection, checksum helper, APK install intent helper.
- GitHub Actions for CI, release builds, and stable-manifest verification.
- Professional GitHub repo docs, templates, README, changelog, and brand assets.

### Security
- No analytics, telemetry, account system, database, or persistent audio storage in the MVP.
