# Testing

## Unit tests

- `AudioFormatDetectorTest`
- `StableReleaseSelectorTest`
- Session cleanup tests planned for v0.2.0
- Summary output contract tests planned for v0.5.0

## Manual smoke test: HONOR Magic V2

1. Install `offlineDebug`.
2. Share a WhatsApp voice note to `tl;dh`.
3. Confirm app opens from Sharesheet.
4. Confirm metadata/fake summary renders.
5. Tap Copy All.
6. Close app.
7. Reopen app and confirm no previous result is visible.
8. Repeat with folded/unfolded device state.

## Updater smoke tests

- No update check in offline flavor.
- Manual check only in updater flavor.
- No silent notification.
- No stale progress UI after rotation/fold-unfold.
- Checksum mismatch blocks install.
