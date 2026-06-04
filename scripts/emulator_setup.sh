#!/usr/bin/env bash
# Install a self-contained Android SDK + emulator + system image and create an
# AVD that matches the target phone (Samsung SM-S901E: 1080x2340 @ 480dpi).
#
# Everything lives under ANDROID_SDK_ROOT so the host stays clean. KVM is used
# for acceleration (host already exposes /dev/kvm).
#
# Usage: scripts/emulator_setup.sh
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-/media/KINGDATA/android-sdk}"
CMDLINE_VER="11076708"   # commandlinetools-linux build
CMDLINE_ZIP="commandlinetools-linux-${CMDLINE_VER}_latest.zip"
CMDLINE_URL="https://dl.google.com/android/repository/${CMDLINE_ZIP}"

API="34"
IMAGE="system-images;android-${API};google_apis;x86_64"
AVD_NAME="s22_openeksin"

mkdir -p "$SDK_ROOT"
cd "$SDK_ROOT"

if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "==> Downloading command-line tools"
    curl -fL -o "$CMDLINE_ZIP" "$CMDLINE_URL"
    rm -rf cmdline-tools/latest cmdline-tools/tmp
    mkdir -p cmdline-tools/tmp
    unzip -q "$CMDLINE_ZIP" -d cmdline-tools/tmp
    mkdir -p cmdline-tools/latest
    mv cmdline-tools/tmp/cmdline-tools/* cmdline-tools/latest/
    rm -rf cmdline-tools/tmp "$CMDLINE_ZIP"
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"

echo "==> Accepting licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

echo "==> Installing platform-tools, emulator, platform, system image"
"$SDKMANAGER" "platform-tools" "emulator" "platforms;android-${API}" "$IMAGE"

echo "==> Creating AVD: $AVD_NAME"
echo "no" | "$AVDMANAGER" create avd \
    --force \
    --name "$AVD_NAME" \
    --package "$IMAGE" \
    --device "pixel_6" >/dev/null

# Match the phone: 1080x2340 @ 480dpi, give it some RAM/cores.
AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
CFG="$AVD_DIR/config.ini"
if [ -f "$CFG" ]; then
    {
        echo "hw.lcd.width=1080"
        echo "hw.lcd.height=2340"
        echo "hw.lcd.density=480"
        echo "hw.ramSize=4096"
        echo "hw.cpu.ncore=4"
        echo "disk.dataPartition.size=6144M"
        echo "hw.keyboard=yes"
    } >> "$CFG"
fi

echo "==> Done."
echo "SDK_ROOT=$SDK_ROOT"
echo "AVD=$AVD_NAME"
echo "Start with: scripts/emulator_run.sh"
