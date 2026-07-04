# Testing

## Unit tests

- `AudioFormatDetectorTest`
- `AudioIngestPolicyTest`
- `StableReleaseSelectorTest`
- Session cleanup tests planned for v0.3.x/v0.4.0
- Summary output contract tests planned for v0.5.0

## Manual smoke test: HONOR Magic V2

1. Install `tldh-<version>.apk` from GitHub Releases.
2. Share a WhatsApp voice note to `tl;dh`.
3. Confirm app opens from Sharesheet.
4. Confirm metadata/fake summary renders.
5. Tap Copy All.
6. Close app.
7. Reopen app and confirm no previous result is visible.
8. Repeat with folded/unfolded device state.

## Updater smoke tests

1. Install the single release APK.
2. Open `tl;dh`.
3. Tap `Nach stabilem Update suchen`.
4. If no newer stable release exists, app should say it is up to date.
5. If a newer stable release exists, app should show it, download only after user confirmation, verify SHA256, and then open Android's installer UI.

There must be no background update check, no silent notification and no stale progress UI after rotation/fold-unfold.
