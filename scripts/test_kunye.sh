#!/usr/bin/env bash
# Smoke-test künye screen on a connected device: open profile, capture screenshots.
set -euo pipefail

SERIAL="${ADB_SERIAL:-$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
PKG="com.drejo.openeksin"
OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/test-screenshots"
mkdir -p "$OUT_DIR"
stamp="$(date +%Y%m%d_%H%M%S)"

adb_cmd() { adb -s "$SERIAL" "$@"; }

shot() {
  local name="$1"
  adb_cmd exec-out screencap -p > "${OUT_DIR}/${stamp}_${name}.png"
  echo "screenshot: ${OUT_DIR}/${stamp}_${name}.png"
}

tap_menu_open_kunye() {
  adb_cmd shell input tap 84 177
  sleep 1
  adb_cmd shell input tap 540 1211
  sleep 4
}

echo "device: $SERIAL"
adb_cmd shell am force-stop "$PKG"
adb_cmd shell am start -n "${PKG}/.MainActivity"
sleep 3
shot "01_home"

tap_menu_open_kunye
shot "02_kunye_entries_tab"

# second tab: "en çok favorilenenler" (right half of tab row under top bar)
adb_cmd shell input tap 650 320
sleep 4
shot "03_kunye_favorited_tab"

# cold start again
adb_cmd shell am force-stop "$PKG"
adb_cmd shell am start -n "${PKG}/.MainActivity"
sleep 3
tap_menu_open_kunye
sleep 2
shot "04_kunye_after_cold_start"

# verify UI has entry-like text (topic titles are usually lowercase Turkish)
if adb_cmd shell uiautomator dump /sdcard/kunye_test.xml >/dev/null 2>&1; then
  adb_cmd shell cat /sdcard/kunye_test.xml | rg -o 'text="[^"]{8,}"' | head -10 || true
fi

# Tap favorited tab, open entry, back — tab should stay on favorited
adb_cmd shell input tap 650 320
sleep 3
adb_cmd shell input tap 540 520
sleep 3
adb_cmd shell input keyevent 4
sleep 2
shot "07_back_to_favorited_tab"
if adb_cmd shell uiautomator dump /sdcard/kunye_tab_test.xml >/dev/null 2>&1; then
  adb_cmd shell cat /sdcard/kunye_tab_test.xml | rg -o 'text="(entry'"'"'ler|en çok favorilenenler)"' || true
fi

echo "done"
