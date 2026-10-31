# Third-party notices

## whisper.cpp (bundled in the APK)

- What: `whisper-cli` from whisper.cpp, cross-compiled for Android arm64-v8a and packaged as `lib/arm64-v8a/libwhisper_cli.so` and `lib/arm64-v8a/libwhisper_cli_dotprod.so`.
- Source: https://github.com/ggml-org/whisper.cpp, tag `v1.9.5` (commit `d1be6fde11ac6e0407606b4e42fe72d34add8037`), built by `scripts/build-whisper-android.sh`.
- License: MIT, full text in [licenses/whisper.cpp-MIT.txt](licenses/whisper.cpp-MIT.txt).

## Whisper models (downloaded at runtime, not bundled)

- What: `ggml-base-q5_1.bin` and `ggml-small-q5_1.bin`, OpenAI Whisper weights converted and quantized for whisper.cpp, downloaded on demand from https://huggingface.co/ggerganov/whisper.cpp at revision `5359861c739e955e79d9a303bcbc70fb988958b1` and verified by SHA-256.
- License: MIT (OpenAI Whisper model weights), see https://github.com/openai/whisper/blob/main/LICENSE.

## Bricolage Grotesque (bundled in the APK and in docs/brand)

- What: static Medium and Bold instances (optical size 36) as `app/src/main/res/font/bricolage_grotesque_*.ttf`; 96 pt Bold and 48 pt Medium instances in `docs/brand/fonts/` for the README banner. Instanced from the variable font with fontTools.
- Source: https://github.com/google/fonts/tree/main/ofl/bricolagegrotesque (upstream https://github.com/ateliertriay/bricolage), Copyright 2022 The Bricolage Grotesque Project Authors.
- License: SIL Open Font License 1.1, full text in [licenses/BricolageGrotesque-OFL.txt](licenses/BricolageGrotesque-OFL.txt).

