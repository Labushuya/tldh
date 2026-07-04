# Release Process

`tl;dh` uses one APK per stable release.

## Release asset naming

```text
tldh-<version>.apk
```

Example:

```text
tldh-0.2.2.apk
```

Legacy split assets such as `tldh-offline-*.apk` and `tldh-updater-*.apk` are no longer produced.

## Automatic release path

```text
push to main
→ tldh - Build, Test and Auto Release
→ Verify Android build
→ Publish stable GitHub release
```

The publish job only runs after the verify job succeeds and only on pushes to `main`.

## Manual/tagged release path

The fallback workflow is:

```text
tldh - Release by Version or Tag
```

It can be triggered manually with a SemVer version or by pushing a tag such as `v0.2.2`.

## Signing

All release APKs must be signed with the same release key to remain update-compatible.

Required GitHub Actions secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The release workflow decodes the keystore into the runner temp directory, builds `assembleRelease`, renames the artifact to `tldh-<version>.apk`, writes `SHA256SUMS.txt`, creates `release-manifest.json`, validates it and publishes a GitHub Release.
