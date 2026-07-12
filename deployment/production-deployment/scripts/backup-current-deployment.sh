#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR backup-current-deployment exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
REMOTE_BACKUP_DIR="${REMOTE_BACKUP_DIR:-/var/backups/hunttech-hrm/deployment-$(date +%Y%m%d-%H%M%S)}"
refuse_wildcard_path "${REMOTE_BACKUP_DIR}"
log "Backup old /app and /app-core deployment metadata on ${REMOTE_HOST}"
run_cmd safe_ssh "${REMOTE_HOST}" "mkdir -p '${REMOTE_BACKUP_DIR}' && cp -a /var/lib/tomcat9/webapps/app '${REMOTE_BACKUP_DIR}/app' && cp -a /var/lib/tomcat9/webapps/app-core '${REMOTE_BACKUP_DIR}/app-core'"
