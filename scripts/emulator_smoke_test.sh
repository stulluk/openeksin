#!/usr/bin/env bash
# Emulator UI smoke test for openeksin (requires logged-in session on emulator).
set -euo pipefail
EMU="${EMU_SERIAL:-emulator-5554}"
PKG="com.drejo.openeksin"
log() { echo "==> $*"; }
tap() { adb -s "$EMU" shell input tap "$1" "$2"; }
text() { adb -s "$EMU" shell input text "$1"; }
key() { adb -s "$EMU" shell input keyevent "$1"; }
back() { key 4; }
dump() { adb -s "$EMU" shell uiautomator dump /sdcard/uitest.xml >/dev/null 2>&1; adb -s "$EMU" shell cat /sdcard/uitest.xml; }

log "launch app"
adb -s "$EMU" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 3

log "open search"
tap 996 224
sleep 2

log "type cursor in search"
tap 540 200
sleep 0.5
text "cursor"
sleep 2

log "pick cursor suggestion"
# tap first list item below search field
tap 540 320
sleep 4

log "open topic menu (short text icon)"
tap 1020 224
sleep 1

log "tap yaz"
tap 540 900
sleep 2

log "compose: type test"
tap 540 400
text "test"
sleep 1

log "send entry"
tap 1020 224
sleep 5

log "find drejomaster entry menu (scroll down first)"
adb -s "$EMU" shell input swipe 540 1800 540 800 400
sleep 1
adb -s "$EMU" shell input swipe 540 1800 540 800 400
sleep 1

# open 3-dot on last visible entry area (approx bottom-right of list)
tap 1020 1900
sleep 1

if dump | rg -q 'düzenle'; then
  log "edit entry"
  tap 540 700
  sleep 2
  # select all and replace - clear field roughly
  adb -s "$EMU" shell input keyevent 123  # move cursor end
  for _ in $(seq 1 20); do adb -s "$EMU" shell input keyevent 67; done
  text "test2"
  sleep 1
  tap 1020 224
  sleep 4
  tap 1020 1900
  sleep 1
fi

if dump | rg -q '>sil<'; then
  log "delete entry"
  tap 540 850
  sleep 1
  tap 800 1200
  sleep 3
fi

log "follow first user on page"
back
sleep 1
tap 1020 600
sleep 1
if dump | rg -q 'takip et'; then
  tap 540 1100
  sleep 2
  tap 1020 600
  sleep 1
  tap 540 1100
  sleep 2
fi

log "logout"
tap 84 224
sleep 1
tap 540 1280
sleep 2

if dump | rg -q 'giriş'; then
  log "logout ok — drawer shows giriş"
  tap 540 1280
  sleep 5
  if dump | rg -qi 'verify you are human|verification failed'; then
    log "login blocked by Turnstile (expected on emulator)"
  fi
fi

log "done"
