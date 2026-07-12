#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR rollback-to-app exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
[[ "${CONFIRMED_NO_HRM_USER_WRITES:-no}" == "yes" ]] || { echo "Automatic rollback forbidden unless no /hrm user writes are confirmed." >&2; exit 60; }
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Rollback to /app before user writes. Databases are not deleted."
run_cmd safe_ssh "${REMOTE_HOST}" "systemctl start tomcat9"
