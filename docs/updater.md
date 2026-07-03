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
