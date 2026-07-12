#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR cleanup-failed-deployment exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
[[ "${ALLOW_CLEANUP_FAILED_HRM:-no}" == "yes" ]] || { echo "Cleanup requires ALLOW_CLEANUP_FAILED_HRM=yes." >&2; exit 70; }
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Cleanup failed /hrm only. /app and /app-core deletion is forbidden."
run_cmd safe_ssh "${REMOTE_HOST}" "rm -f /var/lib/tomcat9/webapps/hrm.war /var/lib/tomcat9/webapps/hrm-core.war"
