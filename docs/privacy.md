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

Session data (the copied audio, the decoded PCM used for snippet playback, whisper's output) lives in one `tldh-session-<uuid>` folder per voice note and is wiped:

- when the user closes the result (X, back, or "Session löschen"); the app then returns to the start screen and confirms the deletion
- when a new voice note is shared or opened
- when processing is cancelled or fails
- at app start for orphaned sessions
- when the app is closed normally

Audio picked with "Audiodatei öffnen" is read through the system file picker's temporary permission; tl;dh does not request storage permissions and keeps no reference to the file after the session.

Hard process kills can bypass Android lifecycle callbacks. Therefore orphan cleanup on next launch is mandatory.

## Background work and notifications

Transcription and downloads run in an app-wide engine. While something is running, tl;dh starts a foreground service (types `mediaProcessing` on Android 15+, `dataSync` before) with a silent progress notification and a partial wake lock, so the work continues when the app is in the background or the display is off. The display is never kept on. The service stops as soon as nothing runs.

When a summary finishes while tl;dh is not visible, it posts a "Zusammenfassung fertig" notification. Notifications contain only generic text, never words from the voice note. On Android 13+ the notification permission is requested once, when background work first starts; if it is denied, work still continues without visible notifications.

## Clipboard

When users tap Copy, Android owns the clipboard content. The app cannot guarantee clipboard lifetime or visibility behavior across all Android versions and OEM skins.
