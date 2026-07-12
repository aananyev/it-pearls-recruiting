#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR start-old-app exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Rollback helper: start old Tomcat application service without modifying it."
run_cmd safe_ssh "${REMOTE_HOST}" "systemctl start tomcat9"
