#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR validate-application-health exit=${rc}" >&2; exit "${rc}"' ERR

URL="${1:-http://hr.hunttech.ru:8080/hrm/}"
echo "$(date '+%Y-%m-%dT%H:%M:%S%z') Checking application HTTP reachability: ${URL}"
curl -I --max-time 10 "${URL}"
