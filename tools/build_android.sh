#!/bin/sh
# Build the local first-room APK from the user's converted assets.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TOOLCHAIN="$ROOT/build/android-tools"
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
    for candidate in "$TOOLCHAIN"/jdk-17*; do
        if [ -x "$candidate/bin/java" ]; then JAVA_HOME="$candidate"; export JAVA_HOME; break; fi
    done
fi
if [ -z "${ANDROID_HOME:-}" ]; then
    ANDROID_HOME="${ANDROID_SDK_ROOT:-$TOOLCHAIN/sdk}"
    export ANDROID_HOME
fi
if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
    echo 'Android SDK 35 is required. Set ANDROID_HOME; see android/README.md.' >&2
    exit 1
fi
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$TOOLCHAIN/gradle-user}"
ANDROID_USER_HOME="${ANDROID_USER_HOME:-$TOOLCHAIN/user}"
export GRADLE_USER_HOME ANDROID_USER_HOME
python3 -I "$ROOT/tools/prepare_android_assets.py"
cd "$ROOT/android"
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
mkdir -p "$ROOT/build/android"
cp app/build/outputs/apk/debug/app-debug.apk "$ROOT/build/android/re2-thor-exploration.apk"
echo "APK: $ROOT/build/android/re2-thor-exploration.apk"
