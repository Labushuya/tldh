# tl;dh STT Bench

Separate Android benchmark app for testing local German STT engines before anything is integrated into the main `tl;dh` app.

## Current release: 0.2.2

Focus: Vosk multi-model testing with optional reference transcript comparison.

## Features

- Separate Android package: `dev.bitsbots.tldhbench`.
- Share target for WhatsApp/Telegram audio.
- Vosk German model catalog with live download/delete/switching.
- Model traffic lights for speed, expected accuracy, and phone suitability.
- Benchmark timings: decode, model load, STT, total, RTF.
- Optional reference text / gold-standard field.
- Automatic WER and CER calculation after each benchmark when a reference is provided.
- Expandable reference vs. recognized transcript view.
- Last 5 benchmark runs stored locally inside the benchmark app.
- Current run reset button; history reset remains separate.

## Reference comparison

Paste the correct transcript into the reference field before starting the benchmark. The app then calculates:

- WER: word error rate.
- CER: character error rate.
- Word substitutions / insertions / deletions.
- A rough label: very good, usable, critical, weak.

This is intended for Common Voice, self-made WhatsApp gold-standard clips, or any audio where the correct transcript is known.

## Privacy

No telemetry. No cloud STT. The reference text and last-5 history are stored locally in the benchmark app only.
