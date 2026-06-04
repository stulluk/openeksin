#!/usr/bin/env bash
# Pull eksisozluk session cookies from a logged-in phone and inject them into
# openeksin on the emulator (debug builds only). Requires sqlite3 and adb.
set -euo pipefail

PHONE_SERIAL="${PHONE_SERIAL:-R5CTA2ZC81L}"
EMU_SERIAL="${EMU_SERIAL:-emulator-5554}"
PKG="com.drejo.openeksin"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "==> Pulling WebView cookies from phone ($PHONE_SERIAL)"
adb -s "$PHONE_SERIAL" exec-out run-as "$PKG" cat app_webview/Default/Cookies >"$TMP/Cookies"

echo "==> Building import_session.json (eksisozluk cookies only)"
python3 - "$TMP/Cookies" "$TMP/import_session.json" <<'PY'
import json, sqlite3, sys

db, out = sys.argv[1], sys.argv[2]
con = sqlite3.connect(db)
rows = con.execute(
    "SELECT host_key, name, value FROM cookies "
    "WHERE host_key LIKE '%eksisozluk.com%' AND length(value) > 0 "
    "AND name NOT LIKE '_ga%'"
).fetchall()
con.close()
cookies = [{"host": h, "name": n, "value": v} for h, n, v in rows]
with open(out, "w", encoding="utf-8") as f:
    json.dump({"cookies": cookies}, f)
print(f"    exported {len(cookies)} cookies")
auth = [c for c in cookies if c["name"] == "a"]
if not auth:
    print("WARNING: no auth cookie 'a' found — is the phone logged in?", file=sys.stderr)
    sys.exit(1)
PY

echo "==> Pushing cookies to emulator ($EMU_SERIAL)"
adb -s "$EMU_SERIAL" shell am force-stop "$PKG"
cat "$TMP/import_session.json" | adb -s "$EMU_SERIAL" shell \
  "run-as $PKG sh -c 'cat > files/import_session.json && chmod 600 files/import_session.json'"

echo "==> Restarting app on emulator"
adb -s "$EMU_SERIAL" shell am start -n "$PKG/.MainActivity"
sleep 4

echo "==> Done. Open the drawer — nick should appear instead of 'giriş'."
