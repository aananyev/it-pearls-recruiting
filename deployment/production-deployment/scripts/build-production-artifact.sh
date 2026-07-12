#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR build-production-artifact exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_approved_execute
REPO_ROOT="$(cd "${DEPLOYMENT_ROOT}/../.." && pwd)"
log "Build task: ./gradlew clean buildWar"
run_cmd bash -lc "cd '${REPO_ROOT}' && ./gradlew clean buildWar --console=plain"
