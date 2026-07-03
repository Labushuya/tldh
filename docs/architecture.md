# Architecture

## Current v0.1.0 shape

```text
ACTION_SEND Intent
  ↓
MainActivity
  ↓
ShareIntentReader
  ↓
AudioIngestor
  ↓
AudioFormatDetector
  ↓
FakeSummaryEngine
  ↓
Compose Result UI
```

## Target architecture

```text
Share Intent
  ↓
SessionManager
  ↓
AudioIngestor
  ↓
AudioDecoder / Normalizer
  ↓
whisper.cpp JNI transcription
  ↓
Local Summarizer
  ↓
Structured Result UI
  ↓
Session Wipe
```

## Key constraints

- Content-URI first.
- No broad storage permissions.
- No runtime network for audio processing.
- No database in MVP.
- HONOR Magic V2 is the primary hardware target.
