#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR stop-hrm exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Stop /hrm context by preserving artifacts for analysis. Does not touch /app."
run_cmd safe_ssh "${REMOTE_HOST}" "test -e /var/lib/tomcat9/webapps/hrm || true"
