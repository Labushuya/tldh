# Release Process

## Versioning

- SemVer: `MAJOR.MINOR.PATCH`
- Git tags: `vMAJOR.MINOR.PATCH`
- Android `versionName`: SemVer
- Android `versionCode`: monotonically increasing integer

## Signing

Release APKs must use the same signing key forever if they should update existing installations.

Required GitHub Secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

## Stable release checklist

- [ ] CI green
- [ ] Tag is SemVer
- [ ] Release is not draft
- [ ] Release is not prerelease
- [ ] APK exists
- [ ] SHA256 exists
- [ ] `release-manifest.json` exists
- [ ] Manifest validates
- [ ] Changelog updated
