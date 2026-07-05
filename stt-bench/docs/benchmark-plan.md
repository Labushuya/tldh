# Benchmark Plan

## Audio classes

1. 5-20s short WhatsApp voice note
2. ~60s ordinary note
3. ~180s long note
4. ~4-5min stress test

## Acceptance gates

- 15s audio: total <= 15s
- 60s audio: total <= 30s
- 180s audio: total <= 120s

## Quality review

Use the transcript foldout with timestamps. Check:

- Did the recognizer preserve names, times, numbers?
- Did it miss negations?
- Are action requests recognizable?
- Would a tl;dh summary based on this transcript be trustworthy?

## Decision

- Fast + acceptable German quality: candidate for tl;dh phone fast mode.
- Fast + weak quality: preview-only or reject.
- Slow: not suitable for phone-first path.
