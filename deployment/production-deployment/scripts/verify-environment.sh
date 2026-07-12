#!/usr/bin/env bash
set -Eeuo pipefail
trap 'rc=$?; echo "$(date '+%Y-%m-%dT%H:%M:%S%z') ERROR verify-environment exit=${rc}" >&2; exit "${rc}"' ERR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "${SCRIPT_DIR}/lib.sh"

usage() {
  echo "Usage: $0 [--dry-run] [--execute]"
  usage_common
}

parse_common_args "$@"
require_manifest
log "Stage 1 read-only environment verification"
log "Local host: $(hostname -f 2>/dev/null || hostname)"
log "Git branch: $(git -C "${DEPLOYMENT_ROOT}/../.." rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
log "Git commit: $(git -C "${DEPLOYMENT_ROOT}/../.." rev-parse --short HEAD 2>/dev/null || echo unknown)"
log "Manifest: ${MANIFEST}"
log "No production changes are performed by this script."
