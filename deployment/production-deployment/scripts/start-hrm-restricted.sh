#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR start-hrm-restricted exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REPO_ROOT="$(cd "${DEPLOYMENT_ROOT}/../.." && pwd)"
HUNTTECH_DB_PROFILE=PRODUCTION bash "${REPO_ROOT}/scripts/validate-db-profile.sh" --profile PRODUCTION >/dev/null
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Start Tomcat only after HRM disabled config is installed. Stage 1 must not run this."
run_cmd safe_ssh "${REMOTE_HOST}" "systemctl start tomcat9"
