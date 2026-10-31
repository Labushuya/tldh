<p align="center">
  <img src="docs/brand/banner.png" alt="tl;dh — too long; didn't hear" width="100%">
</p>

# tl;dh

**too long; didn't hear**

Voice notes are too long. Your time is not. `tl;dh` is an Android Share Target that turns WhatsApp voice notes into a concise, copy-ready brief — locally, offline-first, session-only, and with a manual in-app update check.

> **Status:** `v0.6.0` reply suggestions. Voice notes are transcribed on the device with `whisper.cpp`; tl;dh shows a verbatim TL;DR, questions, dates/numbers and requests, flags uncertain meaning-critical words with one-tap replay, and offers copy-ready replies in three tones plus a note mode. Next is an LLM benchmark in the STT bench for the optional on-device AI pack.

![CI](https://img.shields.io/github/actions/workflow/status/OWNER/tldh/ci.yml?branch=main&label=CI)
![Android](https://img.shields.io/badge/Android-local--first-A50B5E)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF)
![Privacy](https://img.shields.io/badge/Privacy-no%20cloud-34D399)
![License](https://img.shields.io/badge/License-MIT-white)

## What it solves

WhatsApp voice notes are often long, unstructured, and time-expensive. `tl;dh` lets you share an audio file into the app and receive:

- a local transcript where uncertain words are marked (tap to hear that moment)
- TL;DR and key points (questions to you, dates and numbers, tasks)
- warnings for meaning-critical words that were recognized with low confidence (negations, times, numbers, names)
- reply suggestions and a note mode, copied straight to the clipboard

The base pipeline is rule-based and runs on every arm64 phone. An optional on-device AI pack for capable phones is planned after a separate LLM benchmark; it will never be required.

## MVP flow

```text
WhatsApp audio → Android Sharesheet → tl;dh → local inspect → duration gate → whisper.cpp transcription → highlighted transcript → copy result → session wipe
```

## Supported sources

| Source | Status | Notes |
|---|---:|---|
| WhatsApp `.opus` voice notes | Primary | Main target |
| Telegram `.ogg` audio | Secondary | Supported if the same Ogg/Opus path works cleanly |
| Instagram | Out of scope | No useful external audio share flow |
| Facebook Messenger | Out of scope | No useful external audio share flow |

## Privacy model

Core behavior:

- no cloud speech-to-text
- no cloud summarization
- no analytics
- no telemetry
- no account
- no database in the MVP
- no persistent audio/transcript/result storage
- session files wiped on start and explicit close

`tl;dh` is a single APK. The app is fully usable offline. Internet is only used when the user taps the in-app update check or downloads a Whisper model (once, 60 or 190 MB, from a pinned Hugging Face revision, SHA-256 verified).

Important: Android does not guarantee every lifecycle callback after a hard process kill. `tl;dh` therefore wipes orphaned session files at next startup. Clipboard content is controlled by Android after you copy text.

## Single APK release model

There are no split `offline`/`updater` release variants anymore.

The release asset is exactly one APK:

```text
tldh-<version>.apk
```

Example:

```text
tldh-0.3.0.apk
```

This one app includes:

- offline-capable Share Target and audio ingest
- duration guardrails for long voice notes
- local `whisper.cpp` transcription with word-level confidence and snippet playback
- manual stable in-app update check
- APK download with SHA256 verification
- Android installer handoff after explicit user confirmation

No background update polling. No silent installs. No notification-based stale updater UI.

## Technical baseline

- Kotlin `2.4.0`
- Android Gradle Plugin `9.2.1`
- Jetpack Compose BOM `2026.04.01`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 28`
- primary ABI: `arm64-v8a`
- `whisper.cpp` `v1.9.5` (`whisper-cli`, built from source in CI, two CPU variants)
- Whisper models: `base` and `small`, q5_1 quantized, downloaded on demand
- package: `dev.bitsbots.tldh`

## Local build

This repository includes `scripts/gradle.sh`, which uses an existing Gradle installation or bootstraps Gradle locally.

```bash
bash scripts/gradle.sh lintDebug testDebugUnitTest assembleDebug
```

Build the signed release APK in CI with:

```bash
bash scripts/gradle.sh assembleRelease
```

For real signed releases, configure:

```text
ANDROID_KEYSTORE_FILE
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

## Release discipline

- SemVer tags: `v0.3.0`, `v0.3.1`, ...
- `versionName` follows SemVer
- `versionCode` is monotonically increasing
- release APKs are signed with the same key for update compatibility
- GitHub Releases include one APK, SHA256 sums, and `release-manifest.json`

## Stable updater rule

The in-app updater does **not** blindly install `latest`.

A release is eligible only if:

- not draft
- not prerelease
- valid SemVer tag
- exact APK asset `tldh-<version>.apk` exists
- SHA256 exists and verifies
- not yanked
- stable release manifest validates

## Roadmap

| Version | Goal |
|---:|---|
| `0.1.0` | Share Target MVP + fake summarizer |
| `0.2.0` | Audio ingest + Ogg/Opus detection hardening |
| `0.2.1` | Manual stable updater MVP |
| `0.2.2` | Single APK release model with in-app updater |
| `0.2.4` | Duration probing and long-audio guardrails |
| `0.2.5` | Brand refresh, app icon/banner update, bottom breathing room and guarded update downloads |
| `0.3.0` | Local transcription spike with Android on-device file recognition and PCM preparation |
| `0.4.0` | `whisper.cpp` transcription, model download per device class, word confidence + snippet playback, session wipe per share |
| `0.5.0` | Meaning-critical word warnings, rule-based TL;DR and key points (questions, dates/numbers, tasks) |
| `0.6.0` | Reply templates, note mode, clipboard |
| `0.7.x` | Optional on-device AI pack for capable phones, after an LLM benchmark in the STT bench |
| later | Tags + categories |
| `0.9.0` | Testing, benchmarking, RC |
| `1.0.0` | Public stable release |

## Brand

`tl;dh` uses a dark, local-first visual system with deep magenta `#a50b5e` gradients, compressed voice-wave motifs and extra mobile bottom breathing room. Brand assets live in [`docs/brand`](docs/brand/README.md).

## License

MIT. See [LICENSE](LICENSE).


## v0.2.4 UX guardrail fix

Duration warnings are now shown directly below the TL;DR and included in the copied Share protocol output, so longer WhatsApp voice notes visibly carry their local-processing warning.


## v0.2.5 Brand + updater hardening

- README banner and app logo are updated to the new `Long story, short.` brand direction.
- Primary accent shifts toward `#a50b5e`.
- App UI receives more bottom breathing room for large scrollable result cards.
- Manual APK update downloads now keep the device awake during the foreground download and show clearer interruption guidance instead of collapsing into a generic GitHub/network error.
- The v0.3.0 transcription spike is planned from the validated 4:45 min WhatsApp OGG_OPUS Share protocol in `docs/planning/v0.3.0-whisper-spike.md`.


## v0.3.0 Local transcription spike

- Result cards are shorter: TL;DR, optional transcript, guardrails, short points, tags.
- Technical ingest details moved behind a details toggle to reduce text walls.
- The TL;DR card has higher contrast against the dark `#a50b5e` UI direction.
- Shared audio is decoded through Android's media stack and converted to 16 kHz mono PCM for the local recognizer path.
- The spike uses Android's on-device file recognition when the device supports it. If not, the app clearly reports that fallback state and keeps the previous validated ingest/guardrail behavior.
- Native `whisper.cpp` remains the next hardening step after proving the local file-transcription path on the Magic V2.


## v0.4.0 Local Whisper transcription

- The Android on-device recognizer spike is replaced by `whisper.cpp` (`v1.9.5`), cross-compiled for arm64-v8a in CI and bundled as an executable in two CPU variants (baseline armv8-a and armv8.2-a with dotprod/fp16, picked at runtime).
- Whisper models are downloaded once on demand: `base` (≈ 60 MB, every device) or `small` (≈ 190 MB, recommended for phones with a fast CPU and ≥ 6 GB RAM).
- Every word carries whisper's token probability. Uncertain words are highlighted (yellow / red); tapping one replays the moment around it.
- The TL;DR card shows the first sentences as a preview until the rule-based summary of `v0.5.0`.
- Each new share wipes the previous session; copied text is marked as sensitive for the Android clipboard preview.


## v0.5.0 Rule-based summary and meaning checks

- A "Prüfen" card lists uncertain words that can change what was said: negations, times and dates, numbers and amounts, conditions and modal verbs. Each entry shows its sentence and replays that moment with one tap.
- The TL;DR picks the most informative sentences (questions, requests, dates, numbers, repeated keywords) and skips greetings. Key points are grouped into questions to you, dates & numbers, and tasks & requests. Everything is verbatim from the transcript; nothing is generated.
- The bundled whisper-cli binaries are stripped, so the APK gets noticeably smaller.


## v0.6.0 Reply suggestions and note mode

- tl;dh recognizes what a voice note asks for (appointment, request, yes/no or open question, cancellation, thanks, info) and offers three replies: agree, decline or suggest an alternative, defer.
- Tones: kurz, freundlich, förmlich (Sie). Appointment replies reuse the time phrase from the transcript, e.g. "Passt, am Samstag um 17 Uhr bin ich dabei."
- Tap to copy, then paste into WhatsApp. "Als Notiz kopieren" turns the voice note into a short note with date and duration.
