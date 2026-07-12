#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR verify-artifact exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run]"
}

parse_common_args "$@"
REPO_ROOT="$(cd "${DEPLOYMENT_ROOT}/../.." && pwd)"
log "Searching WAR artifacts under ${REPO_ROOT}/build"
find "${REPO_ROOT}" -path '*/build/distributions/*.war' -o -path '*/build/libs/*.war' 2>/dev/null | sort
log "Expected context paths: /hrm and /hrm-core"
