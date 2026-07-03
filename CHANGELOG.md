# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]

### Fixed

- Removed the obsolete `org.jetbrains.kotlin.android` plugin for AGP 9 built-in Kotlin support.
- Removed legacy Kotlin JVM target configuration that is now covered by Android compile options.
- Kept the Compose compiler plugin, because Compose still needs its Kotlin compiler plugin setup.

## [Unreleased]

### Fixed
- Enable AGP 9 resource value generation for flavor-specific `resValue` entries.

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
