#!/usr/bin/env bash
# Export eksisozluk cookies from a desktop Chrome session (Turnstile-friendly).
# Run in your graphical session (not headless). Opens Chrome, auto-fills login via CDP.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CREDS="${ROOT_DIR}/.secrets/credentials.env"
OUT="${ROOT_DIR}/.secrets/import_session.json"
[[ -f "$CREDS" ]] || { echo "missing $CREDS"; exit 1; }
# shellcheck source=/dev/null
source "$CREDS"

VENV="/tmp/cdpvenv"
python3 -m venv "$VENV" 2>/dev/null || true
"$VENV/bin/pip" install -q websocket-client

pkill -f "remote-debugging-port=9333" 2>/dev/null || true
google-chrome-stable --remote-debugging-port=9333 --remote-allow-origins='*' \
  --user-data-dir=/tmp/eksi-chrome-profile --no-first-run \
  "https://eksisozluk.com/giris" >/tmp/eksi-chrome.log 2>&1 &
sleep 8

"$VENV/bin/python3" <<PY
import json, time, urllib.request, websocket, sqlite3, os, shutil, subprocess

email = ${EKSI_EMAIL@Q}
password = ${EKSI_PASSWORD@Q}

pages = json.load(urllib.request.urlopen("http://127.0.0.1:9333/json/list"))
ws_url = pages[0]["webSocketDebuggerUrl"]
ws = websocket.create_connection(ws_url, header=["Origin: http://127.0.0.1:9333"])
mid = 0

def call(method, params=None):
    global mid
    mid += 1
    ws.send(json.dumps({"id": mid, "method": method, "params": params or {}}))
    while True:
        data = json.loads(ws.recv())
        if data.get("id") == mid:
            return data.get("result", {})

call("Runtime.enable")
for _ in range(40):
    n = call("Runtime.evaluate", {"expression": 'document.querySelector("[name=cf-turnstile-response]")?.value?.length||0'})
    if (n.get("result", {}).get("value") or 0) > 10:
        break
    time.sleep(1)

js = f"""(() => {{
  document.getElementById('username').value = {json.dumps(email)};
  document.getElementById('password').value = {json.dumps(password)};
  document.querySelector('button[type=submit]').click();
  return 'ok';
}})()"""
call("Runtime.evaluate", {"expression": js})
time.sleep(8)
state = call("Runtime.evaluate", {"expression": "document.title"})
print("title:", state.get("result", {}).get("value"))
ws.close()
PY

# Pull cookies from Chrome profile
DB="/tmp/eksi-chrome-profile/Default/Cookies"
[[ -f "$DB" ]] || { echo "Chrome cookie DB not found — login may have failed"; exit 1; }

python3 - "$DB" "$OUT" <<'PY'
import json, sqlite3, sys
db, out = sys.argv[1], sys.argv[2]
con = sqlite3.connect(db)
rows = con.execute(
    "SELECT host_key, name, value FROM cookies "
    "WHERE host_key LIKE '%eksisozluk%' AND length(value) > 0"
).fetchall()
con.close()
cookies = [{"host": h, "name": n, "value": v} for h, n, v in rows]
with open(out, "w", encoding="utf-8") as f:
    json.dump({"cookies": cookies}, f)
print(f"exported {len(cookies)} cookies -> {out}")
if not any(c["name"] == "a" for c in cookies):
    raise SystemExit("no auth cookie 'a' — login failed")
PY

echo "Done. Push to phone: cat .secrets/import_session.json | adb shell \"run-as com.drejo.openeksin sh -c 'cat > files/import_session.json'\""
