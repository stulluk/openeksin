#!/usr/bin/env bash
# Boot the openeksin test AVD headless with KVM acceleration and wait until it
# is ready for adb. Leaves the emulator running in the background.
#
# Usage: scripts/emulator_run.sh [--window]
#   --window  show the emulator window instead of headless
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-/media/KINGDATA/android-sdk}"
AVD_NAME="s22_openeksin"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$(command -v adb || echo "$SDK_ROOT/platform-tools/adb")"

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

MODE="-no-window"
if [ "${1:-}" = "--window" ]; then MODE=""; fi

if "$ADB" devices | grep -q emulator-5554; then
    echo "==> Emulator already running"
    exit 0
fi

echo "==> Booting $AVD_NAME ($MODE)"
nohup "$EMULATOR" -avd "$AVD_NAME" \
    $MODE \
    -no-snapshot \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -accel on \
    -netdelay none -netspeed full \
    > /tmp/emulator.log 2>&1 &

echo "==> Waiting for device"
"$ADB" wait-for-device
echo "==> Waiting for boot to complete"
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
done

# Disable lock screen / keep awake so automated testing never hits a keyguard.
"$ADB" shell settings put global window_animation_scale 0
"$ADB" shell settings put global transition_animation_scale 0
"$ADB" shell settings put global animator_duration_scale 0
"$ADB" shell svc power stayon true
"$ADB" shell locksettings set-disabled true 2>/dev/null || true

echo "==> Emulator ready (emulator-5554)"
