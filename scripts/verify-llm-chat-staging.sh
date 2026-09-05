#!/usr/bin/env bash

# Read-only transport smoke check for a dedicated HRM staging environment.
# It does not log in, send chat messages, call an LLM provider, or change data.
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <staging-app-url> [concurrency]" >&2
  echo "Example: $0 https://staging.example/hrm 8" >&2
  exit 2
fi

base_url="${1%/}"
concurrency="${2:-8}"

if [[ ! "$base_url" =~ ^https?://[^/]+/.+ ]]; then
  echo "Ошибка: передайте URL приложения staging, например https://staging.example/hrm" >&2
  exit 2
fi
if [[ ! "$concurrency" =~ ^[1-9][0-9]*$ ]]; then
  echo "Ошибка: concurrency должен быть положительным целым числом" >&2
  exit 2
fi

asset_url="$base_url/VAADIN/vaadinPush.debug.js"
push_url="$base_url/PUSH/"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/llm-chat-staging.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

echo "Проверяю push asset: $asset_url"
asset_status="$(curl -sS -L -o /dev/null -w '%{http_code}' --max-time 15 "$asset_url")"
if [[ "$asset_status" != "200" ]]; then
  echo "FAIL: push asset вернул HTTP $asset_status, ожидался HTTP 200" >&2
  exit 1
fi
echo "OK: push asset HTTP 200"

websocket_headers="$tmp_dir/websocket.headers"
websocket_body="$tmp_dir/websocket.body"
set +e
curl -sS -N --http1.1 --max-time 5 \
  -D "$websocket_headers" -o "$websocket_body" \
  -H 'Upgrade: websocket' \
  -H 'Connection: Upgrade' \
  -H 'Sec-WebSocket-Version: 13' \
  -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
  "$push_url" >/dev/null 2>&1
curl_exit=$?
set -e

http_status="$(awk 'NR == 1 {print $2}' "$websocket_headers" 2>/dev/null || true)"
if [[ "$http_status" != "101" ]]; then
  echo "FAIL: WebSocket handshake вернул HTTP ${http_status:-UNKNOWN}, ожидался HTTP 101" >&2
  exit 1
fi
if [[ "$curl_exit" != "0" && "$curl_exit" != "28" ]]; then
  echo "FAIL: curl завершился с кодом $curl_exit после handshake" >&2
  exit 1
fi
echo "OK: WebSocket handshake HTTP 101"

echo "Проверяю $concurrency параллельных handshake-запросов"
for index in $(seq 1 "$concurrency"); do
  curl -sS -o /dev/null -w '%{http_code}\n' --http1.1 --max-time 5 \
    -H 'Upgrade: websocket' \
    -H 'Connection: Upgrade' \
    -H 'Sec-WebSocket-Version: 13' \
    -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
    "$push_url" > "$tmp_dir/handshake-$index" 2>/dev/null &
done
wait

successes="$(grep -hxc '101' "$tmp_dir"/handshake-* | awk '{sum += $1} END {print sum + 0}')"
if [[ "$successes" != "$concurrency" ]]; then
  echo "FAIL: успешных handshake $successes/$concurrency, ожидалось $concurrency/$concurrency" >&2
  exit 1
fi
echo "OK: параллельные handshake $successes/$concurrency"
echo "Staging transport smoke: PASS"
