# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]

## [0.2.1] - 2026-07-04

### Added
- Add manual stable in-app updater MVP for the updater flavor.
- Add GitHub release client with SHA256 verification and updater APK selection.

### Changed
- Keep core app fully offline while allowing optional manual update checks in updater builds.


### Fixed

- Reprocess shared audio intents delivered through `onNewIntent` so sharing a WhatsApp voice note into an already-running app updates the UI.
- Build SemVer-named signed APKs through the release workflow instead of relying on CI debug artifacts for install testing.
- Allow release workflows to inject `VERSION_NAME` and `VERSION_CODE` into Gradle builds.

## [0.1.0] - 2026-07-03

### Added

- Initial `tl;dh` Android project bootstrap.
- Android Share Target for `audio/*`, `application/ogg`, and defensive `application/octet-stream`.
- Kotlin/Compose UI in the pulsating-purpur brand direction.
- Session manager with orphan cleanup on startup.
- Audio metadata inspection and Ogg/Opus header detection.
- Fake summarizer to prove the end-to-end share/result/copy flow.
- Build flavors: `offline` and `updater`.
- Updater-domain model: SemVer, stable release selection, checksum helper, APK install intent helper.
- GitHub Actions for CI, release builds, and stable-manifest verification.
- Professional GitHub repo docs, templates, README, changelog, and brand assets.

### Security

- No Internet permission in the `offline` flavor.
- No analytics, telemetry, account system, database, or persistent audio storage in the MVP.
