#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR switch-to-hrm exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
[[ "${ALLOW_OPEN_HRM_USERS:-no}" == "yes" ]] || { echo "Need ALLOW_OPEN_HRM_USERS=yes and human phrase approval." >&2; exit 50; }
log "Switch-to-HRM gate passed. Implement environment-specific proxy/user-access action here."
