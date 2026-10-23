#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-$HOME/android-sdk/ndk/27.3.13750724}}"

case "$(uname -m)" in
    aarch64|arm64) HOST=linux-arm64 ;;
    x86_64) HOST=linux-x86_64 ;;
    *) echo "Unsupported host architecture" >&2; exit 1 ;;
esac

CLANG="$NDK/toolchains/llvm/prebuilt/$HOST/bin/clang++"
SOURCE="$ROOT/app/src/main/cpp/archiey_native.cpp"

[ -x "$CLANG" ] || {
    echo "NDK compiler not found: $CLANG" >&2
    exit 1
}

[ -f "$SOURCE" ] || {
    echo "Native source not found" >&2
    exit 1
}

OUTPUT="${1:-$ROOT/app/src/main/jniLibs/arm64-v8a/libarchiey_native.so}"
OUTPUT_DIR=$(dirname "$OUTPUT")

[ -d "$OUTPUT_DIR" ] || {
    echo "Output directory does not exist" >&2
    exit 1
}

TEMP=$(mktemp "$OUTPUT_DIR/.archiey-native-XXXXXXXX.so")

cleanup() {
    rm -f -- "$TEMP"
}
trap cleanup EXIT HUP INT TERM

"$CLANG" \
    --target=aarch64-linux-android26 \
    -shared -fPIC -O2 \
    -static-libstdc++ \
    -o "$TEMP" \
    "$SOURCE"

[ -s "$TEMP" ] || {
    echo "Native compilation produced an empty library" >&2
    exit 1
}

mv -f -- "$TEMP" "$OUTPUT"
trap - EXIT HUP INT TERM

echo "Native build successful: $OUTPUT"
