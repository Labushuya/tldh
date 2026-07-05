# Benchmark Plan

## v0.2.5 focus

Measure Vosk model speed and recognition quality on real German audio.

## Test flow

1. Install/select Vosk model.
2. Share audio into `tl;dh STT Bench`.
3. Paste the correct reference transcript when available.
4. Run benchmark.
5. Review:
   - total time
   - RTF
   - transcript with timestamps
   - WER / CER
   - reference vs recognized text
6. Compare the last 5 benchmark runs in history.
7. For model-level comparison, run the installed gold-standard corpus as a batch and copy the Markdown report.

## Interpretation

- Speed passes when total time stays within the target class.
- Recognition quality is considered usable only if WER/CER are low enough for TL;DR extraction and critical terms such as negations, times, dates, names and quantities are correctly recognized.

## Batch interpretation

- Average RTF shows whether the model has product-level speed headroom.
- Average WER/CER shows whether the model is improving beyond anecdotal single clips.
- Worst WER sample identifies which sentence type currently breaks the model.
- Speed-pass count is only evaluated for samples with a defined target duration class.

## Next candidates

If Vosk small remains too inaccurate, continue with preprocessing, context vocabulary, or larger Vosk models before testing other STT engines.
