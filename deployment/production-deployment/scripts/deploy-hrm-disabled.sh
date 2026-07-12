#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR deploy-hrm-disabled exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute] HRM_WAR HRM_CORE_WAR"
  usage_common
}

ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run|--execute)
      ARGS+=("$1")
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      break
      ;;
  esac
done
parse_common_args "${ARGS[@]}"
require_approved_execute
[[ $# -eq 2 ]] || { usage; exit 2; }
HRM_WAR="$1"
CORE_WAR="$2"
require_file "${HRM_WAR}"
require_file "${CORE_WAR}"
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Deploy /hrm and /hrm-core disabled artifacts; /app is not deleted or overwritten."
run_cmd scp "${HRM_WAR}" "${REMOTE_HOST}:/tmp/hrm.war"
run_cmd scp "${CORE_WAR}" "${REMOTE_HOST}:/tmp/hrm-core.war"
run_cmd safe_ssh "${REMOTE_HOST}" "test ! -e /var/lib/tomcat9/webapps/app.war || true; cp /tmp/hrm.war /var/lib/tomcat9/webapps/hrm.war; cp /tmp/hrm-core.war /var/lib/tomcat9/webapps/hrm-core.war"
