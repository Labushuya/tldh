# Security Policy

## Supported versions

During pre-1.0 development, only the latest stable release is supported.

## Reporting a vulnerability

Please open a private security advisory on GitHub or contact the maintainer privately. Do not publish exploit details before a fix is available.

## Security principles

- local-first processing
- no cloud STT
- no cloud LLM
- no analytics
- no telemetry
- no persistent audio/transcript/result storage
- fully offline-capable core app behavior
- network access only for explicit manual GitHub stable-release update checks
- SHA256 verification before APK install handoff
- no background update polling or silent installation
