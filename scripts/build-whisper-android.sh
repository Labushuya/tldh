#!/usr/bin/env bash
# Builds whisper.cpp's `whisper-cli` for Android arm64-v8a and stages it as lib*.so files, so that
# Android extracts them into the app's native library dir (the one app location it may execute from).
#
# Two CPU variants are built; the app picks one at runtime from /proc/cpuinfo:
#   libwhisper_cli.so          baseline armv8-a, runs on every arm64 phone (e.g. Cortex-A53)
#   libwhisper_cli_dotprod.so  armv8.2-a + dotprod + fp16, noticeably faster on phones since ~2019
#
# Usage: build-whisper-android.sh <ndk-dir> <out-jnilibs-dir> <work-dir>
set -euo pipefail

NDK_DIR="$1"
OUT_DIR="$2"
WORK_DIR="$3"

WHISPER_TAG="v1.9.5"
WHISPER_COMMIT="d1be6fde11ac6e0407606b4e42fe72d34add8037"
ANDROID_PLATFORM="android-28"
VARIANTS=(
  "libwhisper_cli.so:"
  "libwhisper_cli_dotprod.so:armv8.2-a+dotprod+fp16"
)

STAMP_FILE="$WORK_DIR/stamp"
STAMP="$WHISPER_COMMIT ${VARIANTS[*]} $ANDROID_PLATFORM stripped"

all_outputs_present() {
  for variant in "${VARIANTS[@]}"; do
    [ -x "$OUT_DIR/arm64-v8a/${variant%%:*}" ] || return 1
  done
}

if [ -f "$STAMP_FILE" ] && [ "$(cat "$STAMP_FILE")" = "$STAMP" ] && all_outputs_present; then
  echo "whisper-cli for Android is up to date ($WHISPER_TAG)."
  exit 0
fi

if [ ! -f "$NDK_DIR/build/cmake/android.toolchain.cmake" ]; then
  echo "Android NDK not found at '$NDK_DIR'. Install the NDK version set as ndkVersion in app/build.gradle.kts." >&2
  exit 1
fi

STRIP_TOOL="$(ls "$NDK_DIR"/toolchains/llvm/prebuilt/*/bin/llvm-strip 2>/dev/null | head -n 1)"
if [ -z "$STRIP_TOOL" ]; then
  echo "llvm-strip not found in the Android NDK at '$NDK_DIR'." >&2
  exit 1
fi

mkdir -p "$WORK_DIR"
SRC="$WORK_DIR/whisper.cpp"
if [ ! -d "$SRC/.git" ] || [ "$(git -C "$SRC" rev-parse HEAD)" != "$WHISPER_COMMIT" ]; then
  rm -rf "$SRC"
  git clone --quiet --depth 1 --branch "$WHISPER_TAG" https://github.com/ggml-org/whisper.cpp "$SRC"
fi
ACTUAL_COMMIT="$(git -C "$SRC" rev-parse HEAD)"
if [ "$ACTUAL_COMMIT" != "$WHISPER_COMMIT" ]; then
  echo "whisper.cpp $WHISPER_TAG resolved to $ACTUAL_COMMIT, expected $WHISPER_COMMIT. Refusing to build." >&2
  exit 1
fi

JOBS="$(nproc 2>/dev/null || echo 4)"
for variant in "${VARIANTS[@]}"; do
  name="${variant%%:*}"
  arch="${variant#*:}"
  build="$WORK_DIR/build-${name%.so}"
  cmake -S "$SRC" -B "$build" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK_DIR/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM="$ANDROID_PLATFORM" \
    -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF \
    -DGGML_NATIVE=OFF \
    -DGGML_OPENMP=OFF \
    -DGGML_CCACHE=OFF \
    -DGGML_CPU_ARM_ARCH="$arch" \
    -DWHISPER_BUILD_TESTS=OFF \
    -DWHISPER_BUILD_SERVER=OFF \
    -DWHISPER_SDL2=OFF \
    -DWHISPER_CURL=OFF
  cmake --build "$build" --target whisper-cli -j "$JOBS"
  install -D -m 0755 "$build/bin/whisper-cli" "$OUT_DIR/arm64-v8a/$name"
  # Symbols are only useful for debugging and made each binary about 20 MB.
  "$STRIP_TOOL" --strip-all "$OUT_DIR/arm64-v8a/$name"
done

echo "$STAMP" > "$STAMP_FILE"
echo "Built whisper-cli $WHISPER_TAG for Android arm64-v8a: $(ls -l "$OUT_DIR/arm64-v8a" | awk 'NR>1 {print $9" "$5}' | tr '\n' ' ')"
