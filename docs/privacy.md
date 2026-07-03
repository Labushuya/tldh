# Privacy Model

`tl;dh` is designed for local, temporary processing.

## Offline flavor

The `offlineRelease` flavor has no Internet permission and no updater.

## Updater flavor

The `updaterRelease` flavor has Internet permission only for manual GitHub stable-release checks. It must not perform silent background checks in the MVP.

## Session data

Session data is temporary and must be wiped:

- at app start for orphaned sessions
- when the user explicitly deletes the session
- when the app is closed normally

Hard process kills can bypass Android lifecycle callbacks. Therefore orphan cleanup on next launch is mandatory.

## Clipboard

When users tap Copy, Android owns the clipboard content. The app cannot guarantee clipboard lifetime or visibility behavior across all Android versions and OEM skins.
