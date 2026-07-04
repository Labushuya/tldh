# In-App Updater

`tl;dh` ships as one APK. The manual updater is part of that one app.

## UX rules

- Manual check only.
- No background polling.
- No silent notification.
- No automatic installation.
- User must confirm download and Android package installation.

## Stable selection

A release is eligible only if:

- `draft = false`
- `prerelease = false`
- tag is SemVer
- exact APK asset `tldh-<version>.apk` exists
- SHA256 exists in `SHA256SUMS.txt`
- release is not yanked
- manifest validates

Legacy split flavor assets such as `tldh-offline-*.apk` and `tldh-updater-*.apk` are intentionally ignored.

## APK installation

The app downloads the APK to internal cache, verifies SHA256, then opens an Android package install intent via `FileProvider`. The user controls the installation.

## Policy note

`REQUEST_INSTALL_PACKAGES` is sensitive and restricted for Google Play distribution. This GitHub-distributed build intentionally uses it for manual self-updates. If the app is ever Play-distributed, the update model may need to move to Play-managed updates.
