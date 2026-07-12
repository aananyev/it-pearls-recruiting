#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR configure-hrm exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
CONFIG_SOURCE="${DEPLOYMENT_ROOT}/config/hrm-pre-cutover.properties.example"
require_file "${CONFIG_SOURCE}"
log "Install pre-cutover HRM config with automatic DB update and integrations disabled."
run_cmd scp "${CONFIG_SOURCE}" "${REMOTE_HOST}:/tmp/hrm-pre-cutover.properties"
run_cmd safe_ssh "${REMOTE_HOST}" "mkdir -p /opt/app_home/hrm/conf && cp /tmp/hrm-pre-cutover.properties /opt/app_home/hrm/conf/local.app.properties"
