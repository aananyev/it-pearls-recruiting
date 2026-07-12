#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR collect-logs exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run]"
}

parse_common_args "$@"
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Collect sanitized recent Tomcat log excerpts. Do not commit raw production logs."
safe_ssh "${REMOTE_HOST}" "journalctl -u tomcat9 --no-pager -n 200 2>/dev/null | sed -E 's/(password|token|secret)=([^ ]+)/\\1=***REDACTED***/gi' || true"
