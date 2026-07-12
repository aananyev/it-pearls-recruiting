#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR validate-context-path exit=${rc}" >&2; exit "${rc}"' ERR

URL_BASE="${URL_BASE:-http://hr.hunttech.ru:8080}"
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Checking context paths without changing production"
curl -I --max-time 10 "${URL_BASE}/app/" || true
curl -I --max-time 10 "${URL_BASE}/hrm/" || true
