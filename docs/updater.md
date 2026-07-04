# In-App Updater

The updater is available only in the `updaterRelease` flavor.

## UX rules

- Manual check only in MVP.
- No background polling.
- No silent notification.
- No automatic installation.
- User must confirm download and Android package installation.

## Stable selection

A release is eligible only if:

- `draft = false`
- `prerelease = false`
- tag is SemVer
- APK asset exists
- SHA256 exists
- release is not yanked
- manifest validates

## APK installation

The app downloads the APK to internal cache, verifies SHA256, then opens an Android package install intent via `FileProvider`. The user controls the installation.

## Play policy note

`REQUEST_INSTALL_PACKAGES` is sensitive and restricted for Google Play distribution. If the app is ever Play-distributed, the updater flavor may need to be excluded or redesigned around Play-managed updates.


## v0.2.1 Manual Stable Updater

The updater is optional and only exists in the `updater` build flavor. The core app remains fully usable without internet access.

Flow:

1. User opens tl;dh updater flavor.
2. User taps `Nach stabilem Update suchen`.
3. App queries GitHub releases for the configured repository.
4. Draft, prerelease and yanked releases are ignored.
5. The highest SemVer release with `tldh-updater-*.apk` and a SHA256 entry in `SHA256SUMS.txt` is selected.
6. User starts the APK download manually.
7. APK is downloaded to app cache, SHA256 is verified, then Android's installer UI is opened.

There are no background checks, no notification-based stale update UI and no telemetry.
