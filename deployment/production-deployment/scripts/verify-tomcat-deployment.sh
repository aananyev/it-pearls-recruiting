#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR verify-tomcat-deployment exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run]"
}

parse_common_args "$@"
REMOTE_HOST="${REMOTE_HOST:-hr.hunttech.ru}"
log "Read-only Tomcat deployment audit on ${REMOTE_HOST}"
safe_ssh "${REMOTE_HOST}" "set -e; hostname -f || hostname; systemctl is-active tomcat9 || true; ls -la /var/lib/tomcat9/webapps 2>/dev/null || true"
