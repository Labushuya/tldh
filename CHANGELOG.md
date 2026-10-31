# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]


## [0.7.2] - 2026-10-31

### Fixed
- Android 15: sharing a voice note crashed tl;dh right away (the app opened, then you were back in WhatsApp and tl;dh showed the empty start screen). The background service was started with the Android 15 type "media processing", which the AndroidX helper silently dropped; Android 15 rejects a service without type and ends the app. The type now goes straight to the platform.
- If Android refuses the background service for any other reason, tl;dh no longer crashes; the voice note is processed anyway.

### Added
- Emulator smoke test in CI (Android 13, 14, 15): shares an audio file into the app, checks for crashes, the foreground service, leaving the app and rotation.


## [0.7.1] - 2026-10-31

### Fixed
- Rotating the phone (or any other configuration change) no longer restarts a running voice note from the beginning or loses a result. Processing, model and update downloads now live outside the screen in an app-wide engine; the screen only shows its state.
- Reopening tl;dh from recents no longer processes the last shared voice note again.
- The display no longer has to stay on: v0.7.0 kept the screen awake during processing and downloads; that is removed.

### Changed
- Voice notes keep processing when you leave the app or turn the display off. While work runs, a silent progress notification (with "Abbrechen") keeps tl;dh alive; Android hides it for short jobs. When the summary is ready and you are elsewhere, tl;dh posts "Zusammenfassung fertig"; tapping it opens the result. Notifications never contain content of the voice note.
- Android 13+ asks once for the notification permission, the first time background work starts. Without it, processing still continues; only the notifications are hidden.
- Model and update downloads can be cancelled and continue in the background as well.


## [0.7.0] - 2026-10-31

### Added
- Complete redesign of every screen (start, setup, processing, result, settings, error) following the new [DESIGN.md](DESIGN.md): calmer layout, Bricolage Grotesque for headlines, 48 dp touch targets, sentence-case German copy.
- Subtle macOS-like glass: top bars and bottom docks turn to frosted glass while content scrolls underneath (backdrop blur on Android 12+), glass toast and snippet player, lit hairline rims on surfaces and the main button.
- "Audiodatei öffnen": pick audio from storage. Besides Ogg/Opus voice notes, M4A/AAC, MP3, WAV, FLAC, AMR and WebM are now accepted.
- Processing screen with the real waveform of the voice note, step list, live recognized text and a remaining-time estimate.
- Uncertain words get a second cue besides color: dotted underline (unsure) and wavy underline (very unsure), also in the summary and the check rows, with a legend.
- Snippet player pane with stop button; play buttons show an equalizer while playing.
- Settings screen: model choice with recommendation and download progress, delete models (with confirmation), default reply tone, toggle for word marking, updates, privacy statements, licenses.
- New app icon (three bars ending in a dot) as adaptive vector icon with Android 13 themed-icon layer; dark window background and Android 12 splash.
- New README, banner and brand assets.

### Fixed
- Closing a result or tapping "Session löschen" now deletes the session and returns to the start screen with a confirmation; previously the transcript and summary stayed visible.
- No white flash on app start; system bar icons stay light on the dark UI.

### Changed
- The screen stays on while a voice note is processed or a model/update downloads.


## [0.6.0] - 2026-10-31

### Added
- Reply suggestions: rules detect what the voice note asks for (appointment request, request, yes/no question, open question, cancellation, thanks, plain info) and offer three replies (agree, decline or alternative, defer).
- Three tones: kurz, freundlich, förmlich (Sie); the chosen tone is remembered.
- Appointment replies reuse the time phrase from the transcript verbatim, e.g. "Passt, am Samstag um 17 Uhr bin ich dabei."
- Tap a reply to copy it for pasting into WhatsApp; "Als Notiz kopieren" copies TL;DR, questions, dates & numbers and tasks as a note with date and duration.

### Known limitations
- Replies are templates: they do not quote the requested item ("die Getränke") and cover only the most important intent of a message.


## [0.5.0] - 2026-10-31

### Added
- "Prüfen" card: meaning-critical words (negations, times/dates, numbers/amounts, conditions, modal verbs) that whisper recognized with low confidence, most uncertain first, each with its sentence and a button to replay that moment.
- Rule-based TL;DR: the most informative 1–2 sentences, scored by questions, requests, dates, numbers and repeated keywords; greetings and sign-offs are skipped. Everything is taken verbatim from the transcript.
- Key points in three groups: "Fragen an dich", "Termine & Zahlen" (phrases such as "am Samstag um 17 Uhr", "20 Euro") and "Aufgaben & Bitten".
- "Alles kopieren" now includes TL;DR, checks, key points and transcript.

### Changed
- whisper-cli binaries are stripped in the native build, which shrinks the APK and the installed size.
- `PreviewSummaryEngine` is replaced by `RuleBasedSummaryEngine`; unused tag/category/reply fields are removed from the summary model.

### Known limitations
- Names are not flagged yet: German capitalizes every noun, so a rule cannot tell names apart without a name list.
- Confidence thresholds (0.70 / 0.40) are initial values and still need calibration against real voice notes.


## [0.4.0] - 2026-10-31

### Added
- Local transcription with `whisper.cpp` `v1.9.5`: `whisper-cli` is cross-compiled for arm64-v8a in CI (pinned tag and commit) and bundled in two CPU variants, selected at runtime from `/proc/cpuinfo`.
- On-demand Whisper model download (`base` q5_1 ≈ 60 MB, `small` q5_1 ≈ 190 MB) from a pinned Hugging Face revision with size and SHA-256 verification, plus a device-based recommendation.
- Word-level confidence from whisper's token probabilities with three levels; uncertain words are highlighted in the transcript.
- Tap an uncertain word to replay the moment around it from the session audio.
- Processing progress with cancel; whisper runs as a child process, so cancelling stops it immediately.

### Changed
- The Android on-device recognizer spike is removed.
- The TL;DR card is labeled as a preview of the first sentences until the rule-based summary of v0.5.0.
- Each new share wipes the previous session before processing.
- Copied text is flagged as sensitive so Android 13+ hides it in the clipboard preview.
- Roadmap: meaning-critical word warnings and summary next (0.5.0), replies and note mode (0.6.0); tags and categories postponed.


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
