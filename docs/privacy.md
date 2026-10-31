# Privacy Model

`tl;dh` is designed for local, temporary processing.

## Core principle

The app is fully usable offline. Sharing and inspecting WhatsApp/Telegram audio does not require internet access.

The app has internet permission for two manual actions only: the in-app update check and the one-time Whisper model download. Neither runs automatically.

## Network behavior

Network access is only used when the user explicitly taps `Nach stabilem Update suchen` or `Laden` on a Whisper model.

The model download:

- fetches `ggml-base-q5_1.bin` or `ggml-small-q5_1.bin` from a pinned Hugging Face revision
- verifies size and SHA-256 before the model is used
- stores the model in app-private storage (`files/whisper-models`), outside the session folders

Audio is never uploaded. Transcription runs entirely on the device.

The update check:

- queries GitHub Releases for the configured repository
- ignores draft/prerelease/yanked releases
- selects a stable SemVer release with exact `tldh-<version>.apk`
- verifies SHA256 before install handoff

There is no analytics, telemetry, account system or cloud audio processing.

## Session data

Session data is temporary and must be wiped:

- at app start for orphaned sessions
- when the user explicitly deletes the session
- when the app is closed normally

Hard process kills can bypass Android lifecycle callbacks. Therefore orphan cleanup on next launch is mandatory.

## Clipboard

When users tap Copy, Android owns the clipboard content. The app cannot guarantee clipboard lifetime or visibility behavior across all Android versions and OEM skins.
